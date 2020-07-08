package mango.compilation.llvm

import mango.compilation.llvm.LLVMValue.GlobalRef
import mango.compilation.llvm.LLVMValue.StringRef
import mango.interpreter.symbols.FunctionSymbol
import mango.interpreter.symbols.Symbol
import java.util.*
import kotlin.collections.HashMap

private val String.IRCode get() = "c\"" + this
    .replace("\\", "\\5C")
    .replace("\"", "\\22")
    .replace("\n", "\\0A")
    .replace("\t", "\\09")
    .replace("\r", "\\0D") + "\\00\""

open class StringConst(
    val id: String,
    val content: String
) {
    fun lengthInBytes() = content.length + 1
    open fun code() = "@$id = private unnamed_addr constant [${lengthInBytes()} x i8] ${content.IRCode}"
    val ref get() = StringRef(this)
}

data class GlobalVar(
    val name: String,
    val type: LLVMType,
    val value: Any
) : Var {
    fun code() = "@$name = global ${type.code} $value"
    override val ref get() = GlobalRef(name, type)
    override fun allocCode() = "@$name = alloca ${type.code}"
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
        return "${returnType.code} @$name(${paramsAsString.joinToString(", ")})"
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

private var UIDCount = 0
fun newUID() = ".${UIDCount++}"

class ModuleBuilder {
    private val stringConsts = HashMap<String, StringConst>()
    private val importedDeclarations = LinkedList<String>()
    private val importedDefinitions = LinkedList<String>()
    private val declarations = LinkedList<FunctionDeclaration>()
    private val functions = LinkedList<FunctionBuilder>()
    private val structs = LinkedList<LLVMStruct>()
    private val globalVariables = LinkedList<GlobalVar>()

    fun intGlobalVariable (name: String, type: LLVMType = LLVMType.I32, value: Int = 0): GlobalVar {
        val gvar = GlobalVar(name, type, value)
        globalVariables.add(gvar)
        return gvar
    }

    fun floatGlobalVariable (name: String, type: LLVMType = LLVMType.Float, value: Float = 0.0f): GlobalVar {
        val gvar = GlobalVar(name, type, value)
        globalVariables.add(gvar)
        return gvar
    }

    fun globalVariable (name: String, type: LLVMType, value: Any): GlobalVar {
        val gvar = GlobalVar(name, type, value)
        globalVariables.add(gvar)
        return gvar
    }

    fun cStringConstForContent (content: String): StringConst {
        if (!stringConsts.containsKey(content)) {
            stringConsts[content] = StringConst(".str.${stringConsts.size}", content)
        }
        return stringConsts[content]!!
    }

    fun addDeclaration (declaration: FunctionDeclaration) {
        declarations.add(declaration)
    }

    fun declareStruct (name: String, types: Array<LLVMType>) {
        structs.add(LLVMStruct(name, types))
    }

    fun addImportedDeclaration (symbol: FunctionSymbol) {
        val returnType = if (symbol.type.kind == Symbol.Kind.Struct) LLVMType.Ptr(LLVMType.valueOf(symbol.type)) else LLVMType.valueOf(symbol.type)
        importedDeclarations.add("declare ${returnType.code} @${symbol.meta.cName ?: symbol.path}(${symbol.parameters.joinToString(", ") {
            val type = LLVMType.valueOf(it.type)
            (if (it.type.kind == Symbol.Kind.Struct)
                LLVMType.Ptr(type)
            else type).code
        }})")
    }

    fun addImportedDefinition (code: String) {
        importedDefinitions.add(code)
    }

    fun createFunction (symbol: FunctionSymbol): FunctionBuilder {
        val function = FunctionBuilder(this, List(symbol.parameters.size) {
            val type = LLVMType.valueOf(symbol.parameters[it].type)
            (if (symbol.parameters[it].type.kind == Symbol.Kind.Struct)
                LLVMType.Ptr(type)
            else type)
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