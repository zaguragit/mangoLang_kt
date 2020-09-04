package mango.compilation.llvm

import mango.compilation.llvm.LLVMValue.LocalRef
import mango.interpreter.symbols.FunctionSymbol
import mango.interpreter.symbols.TypeSymbol
import mango.util.EmitterError
import java.util.*

data class Label(val name: String)

interface Var {
    fun allocCode(): String
    val ref: LLVMValue
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
        val tempValue = TmpVal(".tmp${functionBuilder.tmpIndex()}", value)
        addInstruction(tempValue)
        return tempValue
    }

    inline fun tmpVal(name: String, value: LLVMInstruction): TmpVal {
        val tempValue = TmpVal(name, value)
        addInstruction(tempValue)
        return tempValue
    }

    fun getStructField(struct: LLVMValue, i: Int, field: TypeSymbol.StructTypeSymbol.Field): LLVMInstruction {
        val loadedStruct = tmpVal(GetPtr((struct.type as LLVMType.Ptr).element, struct, LLVMValue.Int(0, LLVMType.I64), LLVMValue.Int(i, LLVMType.I32))).ref
        val type = LLVMType[field.type]
        return Load(loadedStruct, type)
    }

    inline fun ret() = addInstruction(RetVoid())
    inline fun ret(value: LLVMValue) = addInstruction(Ret(value))

    inline fun conditionalJump(
        condition: LLVMValue,
        name: String,
        antiName: String
    ) = addInstruction(If(condition, name, antiName))

    inline fun jump(name: String) = addInstruction(Jmp(name))

    inline fun alloc(type: LLVMType) = tmpVal(Alloc(type)).ref

    inline fun alloc(name: String, type: LLVMType) = tmpVal(name, Alloc(type)).ref

    fun code() = (if (name != null) "$name:\n    " else "") + instructions.joinToString(separator = "\n    ") { it.code }

    inline fun cStringConstForContent(content: String) = functionBuilder.cStringConstForContent(content)
    inline fun stringConstForContent(content: String): GlobalVar = functionBuilder.stringConstForContent(content)
    inline fun assignVar(variable: Var, value: LLVMValue) = addInstruction(Store(value, variable.ref))

    fun label(): Label {
        if (name == null) throw EmitterError("Label name can't be null")
        return Label(name)
    }

    inline fun load(value: LLVMValue): LocalRef {
        val tempValue = tmpVal(Load(value, (value.type as LLVMType.Ptr).element))
        return tempValue.ref
    }

    inline fun load(name: String, value: LLVMValue): LocalRef {
        val tempValue = tmpVal(name, Load(value, (value.type as LLVMType.Ptr).element))
        return tempValue.ref
    }

    inline fun store(destination: LLVMValue, value: LLVMValue) {
        addInstruction(Store(value, destination))
    }
}

class FunctionBuilder(
    val moduleBuilder: ModuleBuilder,
    val paramTypes: List<LLVMType>,
    val symbol: FunctionSymbol
) {
    val returnType = LLVMType[symbol.returnType]

    private val blocks = LinkedList<BlockBuilder>()
    private val attributes = LinkedList<String>()

    init {
        blocks.add(BlockBuilder(this))
    }

    private var nextTmpIndex = 0
    fun tmpIndex () = nextTmpIndex++

    fun addAttribute (string: String) = attributes.add(string)

    fun code() =
        "define ${returnType.code} @\"${symbol.mangledName()}\"(${paramTypes.map(LLVMType::code).joinToString(separator = ", ")}) ${attributes.joinToString(" ")} {\n" +
        //"    ${variables.joinToString("\n    ") { it.allocCode() }}\n" +
        "    ${blocks.joinToString("\n    ") { it.code() }}\n}\n"


    fun entryBlock (): BlockBuilder = blocks.first

    fun createBlock (name: String?): BlockBuilder {
        val block = BlockBuilder(this, name)
        blocks.add(block)
        return block
    }

    fun cStringConstForContent (content: String) = moduleBuilder.cStringConstForContent(content)
    fun stringConstForContent (content: String) = moduleBuilder.stringConstForContent(content)

    fun paramReference (index: Int): LLVMValue {
        if (index < 0 || index >= paramTypes.size) {
            throw EmitterError("Expected an index between 0 and ${paramTypes.size - 1}, found $index")
        }
        val type = paramTypes[index]
        return LocalRef("$index", type)
    }
}
