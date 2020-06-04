package mango.compilation.llvm

import mango.interpreter.symbols.FunctionSymbol
import java.util.*

data class Label(val name: String)

interface Variable {
    fun allocCode(): String
    val ref: LLVMValue
}

class LocalVariable(val name: String, val type: LLVMType) : Variable {
    override fun allocCode() = "%$name = alloca ${type.code}"
    override val ref get() = LocalValueRef(name, LLVMType.Pointer(type))
}

class BlockBuilder(
        val functionBuilder: FunctionBuilder,
        val name: String? = null
) {
    private val instructions = LinkedList<LLVMInstruction>()

    fun lastOrNull() = instructions.lastOrNull()

    fun addInstruction(instruction: LLVMInstruction) {
        instructions.add(instruction)
    }

    fun tempValue(value: LLVMInstruction): TempValue {
        val tempValue = TempValue("tmpValue${functionBuilder.tmpIndex()}", value)
        addInstruction(tempValue)
        return tempValue
    }

    fun code() = (if (name != null) "$name:\n    " else "") + instructions.joinToString(separator = "\n    ") { it.code }

    fun stringConstForContent(content: String): StringConst {
        return this.functionBuilder.stringConstForContent(content)
    }

    fun addVariable(type: LLVMType, name: String): Variable {
        return this.functionBuilder.addLocalVariable(type, name)
    }

    fun assignVariable(variable: Variable, value: LLVMValue) {
        this.addInstruction(Store(value, variable.ref))
    }

    fun label(): Label {
        if (name == null) throw UnsupportedOperationException()
        return Label(name)
    }

    fun load(value: LLVMValue): LLVMValue {
        val tempValue = tempValue(Load(value))
        return tempValue.reference()
    }

}

class FunctionBuilder(
        val moduleBuilder: ModuleBuilder,
        val paramTypes: List<LLVMType>,
        val symbol: FunctionSymbol
) {
    val returnType = LLVMType.valueOf(symbol.type)
    private val variables = LinkedList<LocalVariable>()
    private var nextTmpIndex = 0
    private val blocks = LinkedList<BlockBuilder>()
    private val attributes = LinkedList<String>()

    init {
        blocks.add(BlockBuilder(this))
    }

    fun tmpIndex() = nextTmpIndex++

    fun addAttribute(string: String) = attributes.add(string)

    fun addLocalVariable(type: LLVMType, name: String): LocalVariable {
        val variable = LocalVariable(name, type)
        variables.add(variable)
        return variable
    }

    fun code(): String {
        return """|define ${returnType.code} @${symbol.name}(${paramTypes.map(LLVMType::code).joinToString(separator = ", ")}) ${attributes.joinToString(" ")} {
                  |    ${variables.joinToString("\n    ") { it.allocCode() }}
                  |    ${blocks.joinToString("\n    ") { it.code() }}
                  |}
                  |""".trimMargin("|")
    }


    fun entryBlock(): BlockBuilder = blocks.first
    fun addInstruction(instruction: LLVMInstruction) = entryBlock().addInstruction(instruction)
    fun tempValue(value: LLVMInstruction) = entryBlock().tempValue(value)

    fun createBlock(name: String?): BlockBuilder {
        val block = BlockBuilder(this, name)
        blocks.add(block)
        return block
    }

    fun stringConstForContent(content: String): StringConst = moduleBuilder.stringConstForContent(content)

    fun paramReference(index: Int): LLVMValue {
        if (index < 0 || index >= paramTypes.size) {
            throw IllegalArgumentException("Expected an index between 0 and ${paramTypes.size - 1}, found $index")
        }
        return LocalValueRef("$index", paramTypes[index])
    }
}
