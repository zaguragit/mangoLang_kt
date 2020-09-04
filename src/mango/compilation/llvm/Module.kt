package mango.compilation.llvm

import mango.compilation.llvm.LLVMValue.GlobalRef
import mango.interpreter.symbols.FunctionSymbol
import mango.interpreter.symbols.TypeSymbol
import java.util.*
import kotlin.collections.HashMap

private val String.IRCode get() = "c\"" + this
    .replace("\\", "\\5C")
    .replace("\"", "\\22")
    .replace("\n", "\\0A")
    .replace("\t", "\\09")
    .replace("\r", "\\0D") + "\\00\""

open class ArrayConst(
    val id: String,
    val type: LLVMType,
    val content: List<LLVMValue>
) {
    fun lengthInBytes() = content.size
    open fun code() = "@$id = unnamed_addr constant [${lengthInBytes()} x ${type.code}] [${content.joinToString(", ") { it.type.code + " " + it.code }}]"
    val ref get() = LLVMValue.ArrayRef(this)
}

open class GlobalVar(
    val name: String,
    val value: LLVMValue
) : Var {
    open fun code() = "@$name = global ${value.type.code} ${value.code}"
    override val ref get() = GlobalRef(name, value.type)
    override fun allocCode() = "@$name = alloca ${value.type.code}"
}

class ConstVal(
    name: String,
    value: LLVMValue
) : GlobalVar(name, value) {
    override fun code() = "@$name = constant ${value.type.code} ${value.code}"
}

data class FunctionDeclaration(
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
    fun code() = "%.struct.$name = type { ${types.joinToString(", ") { it.code }} }"
}

class ModuleBuilder {
    private val stringConsts = HashMap<String, ArrayConst>()
    private val importedDeclarations = LinkedList<String>()
    private val importedDefinitions = LinkedList<String>()
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
            stringConsts[content] = ArrayConst(".str.${stringConsts.size}", LLVMType.I16, content.map { LLVMValue.Int(it.toInt(), LLVMType.I16) })
        }
        return stringConsts[content]!!
    }

    fun stringConstForContent (content: String): GlobalVar {
        val wasAlreadyDeclared = stringConsts.containsKey(content)
        val chars = cStringConstForContent(content)
        val length = LLVMValue.Int(content.length, LLVMType.I32)
        val type = LLVMType[TypeSymbol.String].element as LLVMType.Struct
        val const = ConstVal(chars.id + ".struct", LLVMValue.Struct(type, arrayOf(length, chars.ref)))
        if (!wasAlreadyDeclared) {
            globalVariables.add(const)
        }
        return const
    }

    fun addDeclaration (declaration: FunctionDeclaration) {
        declarations.add(declaration)
    }

    fun declareStruct (name: String, types: Array<LLVMType>) {
        structs.add(LLVMStruct(name, types))
    }

    fun addImportedDeclaration (symbol: FunctionSymbol) {
        val returnType = LLVMType[symbol.returnType]
        importedDeclarations.add("declare ${returnType.code} @\"${symbol.mangledName()}\"(${symbol.parameters.joinToString(", ") {
            LLVMType[it.type].code
        }})")
    }

    fun addImportedDefinition (code: String) {
        importedDefinitions.add(code)
    }

    fun createFunction (symbol: FunctionSymbol): FunctionBuilder {
        val function = FunctionBuilder(this, List(symbol.parameters.size) {
            LLVMType[symbol.parameters[it].type]
        }, symbol)
        functions.add(function)
        return function
    }

    fun code() =
        "${structs.joinToString("\n") { it.code() }}\n" +
        "${stringConsts.values.joinToString("\n") { it.code() }}\n" +
        "${globalVariables.joinToString("\n") { it.code() }}\n" +
        "${importedDefinitions.joinToString("\n")}\n" +
        "${declarations.joinToString("\n") { it.code() }}\n" +
        "${functions.joinToString("\n") { it.code() }}\n" +
        "${importedDeclarations.joinToString("\n")}\n"
}