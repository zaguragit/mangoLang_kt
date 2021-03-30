package mango.cli.emission.llvm

import mango.cli.emission.llvm.LLVMValue.GlobalRef
import mango.compiler.symbols.CallableSymbol
import mango.compiler.symbols.TypeSymbol
import java.util.*
import kotlin.collections.HashMap

open class ArrayConst(
    val id: String,
    val type: LLVMType,
    val content: List<LLVMValue>
) {
    fun lengthInBytes() = content.size
    open fun code() = "@\"$id\" = unnamed_addr constant [${lengthInBytes()} x ${type.code}] [${content.joinToString(", ") { it.type.code + " " + it.code }}]"
    val ref get() = LLVMValue.ArrayRef(this)
}

open class GlobalVar(
    val name: String,
    val value: LLVMValue
) : Var {
    open fun code() = "@\"$name\" = global ${value.type.code} ${value.code}"
    override val ref get() = GlobalRef(name, value.type)
    override fun allocCode() = "@\"$name\" = alloca ${value.type.code}"
}

class ConstVal(
    name: String,
    value: LLVMValue
) : GlobalVar(name, value) {
    override fun code() = "@\"$name\" = constant ${value.type.code} ${value.code}"
}

class FunctionDeclaration(
    val name: String,
    val returnType: LLVMType,
    val paramTypes: List<LLVMType>,
    val varargs: Boolean = false
) {
    fun signature(): String {
        val paramsAsString = LinkedList<String>()
        paramsAsString.addAll(paramTypes.map(LLVMType::code))
        if (varargs) {
            paramsAsString.add("...")
        }
        return "${returnType.code} @\"$name\"(${paramsAsString.joinToString(", ")})"
    }

    fun ptrSignature(): String {
        val paramsAsString = LinkedList<String>()
        paramsAsString.addAll(paramTypes.map(LLVMType::code))
        if (varargs) {
            paramsAsString.add("...")
        }
        return "${returnType.code} (${paramsAsString.joinToString(", ")})*"
    }

    fun code() = "declare ${signature()}"
}

class LLVMStruct(
    val name: String,
    val types: Array<LLVMType>
) {
    fun code() = "%\".type.$name\" = type { ${types.joinToString(", ") { it.code }} }"
}

class ModuleBuilder {
    private val stringConsts = HashMap<String, ArrayConst>()
    private val importedDeclarations = LinkedList<String>()
    private val declarations = LinkedList<FunctionDeclaration>()
    private val functions = LinkedList<FunctionBuilder>()
    private val structs = LinkedList<LLVMStruct>()
    private val globalVariables = LinkedList<GlobalVar>()

    fun intGlobalVariable (name: String, type: LLVMType = LLVMType.I32, value: Int = 0): GlobalVar {
        val gvar = GlobalVar(name, LLVMValue.Int(value, type))
        globalVariables.add(gvar)
        return gvar
    }

    fun floatGlobalVariable (name: String, type: LLVMType = LLVMType.Float, value: Float = 0.0f): GlobalVar {
        val gvar = GlobalVar(name, LLVMValue.Float(value, type))
        globalVariables.add(gvar)
        return gvar
    }

    fun globalVariable (name: String, value: LLVMValue): GlobalVar {
        val g = GlobalVar(name, value)
        globalVariables.add(g)
        return g
    }

    fun constantValue (name: String, value: LLVMValue): GlobalVar {
        val g = ConstVal(name, value)
        globalVariables.add(g)
        return g
    }

    fun cStringConstForContent (content: String): ArrayConst {
        if (!stringConsts.containsKey(content)) {
            stringConsts[content] = ArrayConst(".cs.${stringConsts.entries.size}", LLVMType.I16, content.map { LLVMValue.Int(it.toInt(), LLVMType.I16) })
        }
        return stringConsts[content]!!
    }

    fun stringConstForContent (content: String): GlobalVar {
        val wasAlreadyDeclared = stringConsts.containsKey(content)
        val chars = cStringConstForContent(content)
        val length = LLVMValue.Int(content.length, LLVMType.I32)
        val type = (LLVMType[TypeSymbol.String] as LLVMType.Ptr<*>).element as LLVMType.Struct
        val const = ConstVal(".s.${stringConsts.entries.size}", LLVMValue.Struct(type, arrayOf(length, chars.ref)))
        if (!wasAlreadyDeclared) {
            globalVariables.add(const)
        }
        return const
    }

    fun declareStruct (name: String, types: Array<LLVMType>) {
        structs.add(LLVMStruct(name, types))
    }

    fun addDeclaration (declaration: FunctionDeclaration) {
        declarations.add(declaration)
    }

    fun addDeclaration (symbol: CallableSymbol) {
        declarations.add(FunctionDeclaration(
            symbol.mangledName(),
            LLVMType[symbol.returnType],
            symbol.type.args.map { LLVMType[it] }
        ))
    }

    fun hasFunctionName (name: String) = declarations.find { it.name == name } != null

    fun addImportedDeclaration (symbol: CallableSymbol) {
        val returnType = LLVMType[symbol.returnType]
        importedDeclarations.add("declare ${returnType.code} @\"${symbol.mangledName()}\"(${symbol.parameters.joinToString(", ") {
            LLVMType[it.type].code
        }})")
    }

    fun createFunction (symbol: CallableSymbol): FunctionBuilder {
        val function = FunctionBuilder(this, symbol.type, symbol.parameters, symbol.mangledName())
        functions.add(function)
        return function
    }

    fun createLibraryFunction(moduleName: String): FunctionBuilder {
        val function = FunctionBuilder(this, TypeSymbol.Fn(TypeSymbol.Void, emptyList()), emptyArray(), ".libInit.$moduleName")
        functions.add(function)
        return function
    }

    fun code() =
        "${structs.joinToString("\n") { it.code() }}\n" +
        "${stringConsts.values.joinToString("\n") { it.code() }}\n" +
        "${globalVariables.joinToString("\n") { it.code() }}\n" +
        "${declarations.joinToString("\n") { it.code() }}\n" +
        "${functions.joinToString("\n") { it.code() }}\n" +
        "${importedDeclarations.joinToString("\n")}\n"
}