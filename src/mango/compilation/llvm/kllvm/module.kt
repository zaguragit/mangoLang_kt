package mango.compilation.llvm.kllvm

import mango.interpreter.symbols.FunctionSymbol
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
    open fun IRDeclaration() = "@$id = private unnamed_addr constant [${lengthInBytes()} x i8] ${content.IRCode}"
    val ref get() = StringReference(this)
}

data class GlobalVariable(
        val name: String,
        val type: LLVMType,
        val value: Any
) : Variable {
    fun IRDeclaration(): String {
        return "@$name = global ${type.code} $value"
    }
    override val ref get() = GlobalValueRef(name, type)
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

    fun IRDeclaration() = "declare ${signature()}"
}

private var UIDCount = 0
fun newUID() = ".${UIDCount++}"

class ModuleBuilder {
    private val stringConsts = HashMap<String, StringConst>()
    private val importedDeclarations = LinkedList<String>()
    private val importedDefinitions = LinkedList<String>()
    private val declarations = LinkedList<FunctionDeclaration>()
    private val functions = LinkedList<FunctionBuilder>()
    private val globalVariables = LinkedList<GlobalVariable>()

    fun intGlobalVariable(name: String, type: LLVMType = LLVMType.I32, value: Int = 0): GlobalVariable {
        val gvar = GlobalVariable(name, type, value)
        globalVariables.add(gvar)
        return gvar
    }

    fun floatGlobalVariable(name: String, type: LLVMType = LLVMType.Float, value: Float = 0.0f): GlobalVariable {
        val gvar = GlobalVariable(name, type, value)
        globalVariables.add(gvar)
        return gvar
    }

    fun stringGlobalVariable(name: String, type: LLVMType = LLVMType.Pointer(LLVMType.I8), value: Any = Null(LLVMType.Pointer(LLVMType.I8))): GlobalVariable {
        val gvar = GlobalVariable(name, type, value)
        globalVariables.add(gvar)
        return gvar
    }

    fun globalVariable(name: String, type: LLVMType, value: Any): GlobalVariable {
        val gvar = GlobalVariable(name, type, value)
        globalVariables.add(gvar)
        return gvar
    }

    fun stringConstForContent(content: String): StringConst {
        if (!stringConsts.containsKey(content)) {
            stringConsts[content] = StringConst(".str${stringConsts.size}", content)
        }
        return stringConsts[content]!!
    }

    fun addDeclaration(declaration: FunctionDeclaration) {
        declarations.add(declaration)
    }

    fun addImportedDeclaration(code: String) {
        importedDeclarations.add(code)
    }

    fun addImportedDefinition(code: String) {
        importedDefinitions.add(code)
    }

    fun createFunction(symbol: FunctionSymbol): FunctionBuilder {
        val function = FunctionBuilder(this, List(symbol.parameters.size) {
            LLVMType.valueOf(symbol.parameters[it].type)
        }, symbol)
        functions.add(function)
        return function
    }

    fun code(): String {
        return "${stringConsts.values.map { it.IRDeclaration() }.joinToString("\n")}\n" +
                "${globalVariables.map { it.IRDeclaration() }.joinToString("\n")}\n" +
                "${importedDefinitions.joinToString("\n")}\n" +
                "${declarations.map { it.IRDeclaration() }.joinToString("\n")}\n" +
                "${functions.map { it.code() }.joinToString("\n")}\n" +
                "${importedDeclarations.joinToString("\n")}\n"
    }
}