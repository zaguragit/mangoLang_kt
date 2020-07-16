package mango.compilation.llvm

import mango.compilation.llvm.LLVMValue.LocalRef
import mango.interpreter.symbols.FunctionSymbol
import mango.interpreter.symbols.Symbol
import mango.interpreter.symbols.TypeSymbol
import java.util.*

data class Label(val name: String)

interface Var {
    fun allocCode(): String
    val ref: LLVMValue
}

class Alloc(
    val name: String,
    val type: LLVMType
) : Var {
    override fun allocCode() = "%$name = alloca ${type.code}"
    override val ref get() = LocalRef(name, LLVMType.Ptr(type))
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

    inline fun tmpVal(value: LLVMInstruction): TmpVal {
        val tempValue = TmpVal("tmp${functionBuilder.tmpIndex()}", value)
        addInstruction(tempValue)
        return tempValue
    }

    inline fun tmpVal(name: String, value: LLVMInstruction): TmpVal {
        val tempValue = TmpVal(name, value)
        addInstruction(tempValue)
        return tempValue
    }

    fun getStructField(struct: LLVMValue, i: Int, field: TypeSymbol.StructTypeSymbol.Field): LLVMValue {
        val loadedStruct = tmpVal(GetPtr((struct.type as LLVMType.Ptr).element, struct, LLVMValue.Int(i, LLVMType.I32))).ref
        val type = LLVMType.valueOf(field.type)
        return tmpVal(Load(loadedStruct, if (field.type.kind == Symbol.Kind.Struct) LLVMType.Ptr(type) else type)).ref
    }

    inline fun ret() = addInstruction(RetVoid())
    inline fun ret(value: LLVMValue) = addInstruction(Ret(value))

    inline fun conditionalJump(
        condition: LLVMValue,
        name: String,
        antiName: String
    ) = addInstruction(If(condition, name, antiName))

    inline fun jump(name: String) = addInstruction(Jmp(name))

    inline fun alloc(name: String, type: LLVMType) = functionBuilder.alloc(type, name)

    fun code() = (if (name != null) "$name:\n    " else "") + instructions.joinToString(separator = "\n    ") { it.code }

    inline fun cStringConstForContent(content: String) = functionBuilder.cStringConstForContent(content)
    inline fun stringConstForContent(content: String): GlobalVar = functionBuilder.stringConstForContent(content)
    inline fun assignVar(variable: Var, value: LLVMValue) = addInstruction(Store(value, variable.ref))

    fun label(): Label {
        if (name == null) throw UnsupportedOperationException()
        return Label(name)
    }

    inline fun load(value: LLVMValue): LLVMValue {
        val tempValue = tmpVal(Load(value, (value.type as LLVMType.Ptr).element))
        return tempValue.ref
    }
}

class FunctionBuilder(
    val moduleBuilder: ModuleBuilder,
    val paramTypes: List<LLVMType>,
    val symbol: FunctionSymbol
) {
    val returnType: LLVMType
    init {
        val type = LLVMType.valueOf(symbol.type)
        returnType = (if (symbol.type.kind == Symbol.Kind.Struct)
            LLVMType.Ptr(type)
        else type)
    }
    private val variables = LinkedList<Alloc>()
    private val blocks = LinkedList<BlockBuilder>()
    private val attributes = LinkedList<String>()

    init {
        blocks.add(BlockBuilder(this))
    }

    private var nextTmpIndex = 0
    fun tmpIndex () = nextTmpIndex++

    fun addAttribute (string: String) = attributes.add(string)

    fun alloc (type: LLVMType, name: String): Alloc {
        val variable = Alloc(name, type)
        variables.add(variable)
        return variable
    }

    fun code() =
        "define ${returnType.code} @${if (symbol.meta.isEntry) "main" else symbol.path}(${paramTypes.map(LLVMType::code).joinToString(separator = ", ")}) ${attributes.joinToString(" ")} {\n" +
        "    ${variables.joinToString("\n    ") { it.allocCode() }}\n" +
        "    ${blocks.joinToString("\n    ") { it.code() }}\n}\n"


    fun entryBlock (): BlockBuilder = blocks.first
    fun addInstruction (instruction: LLVMInstruction) = entryBlock().addInstruction(instruction)
    fun tempValue (value: LLVMInstruction) = entryBlock().tmpVal(value)

    fun createBlock (name: String?): BlockBuilder {
        val block = BlockBuilder(this, name)
        blocks.add(block)
        return block
    }

    fun cStringConstForContent (content: String) = moduleBuilder.cStringConstForContent(content)
    fun stringConstForContent (content: String) = moduleBuilder.stringConstForContent(content)

    fun paramReference (index: Int): LLVMValue {
        if (index < 0 || index >= paramTypes.size) {
            throw IllegalArgumentException("Expected an index between 0 and ${paramTypes.size - 1}, found $index")
        }
        val type = paramTypes[index]
        return LocalRef("$index", type)
    }
}
