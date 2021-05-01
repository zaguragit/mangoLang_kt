package mango.cli.emission.llvm

import mango.cli.emission.llvm.LLVMEmitter.LLVMEmitterError
import mango.cli.emission.llvm.LLVMValue.LocalRef
import mango.compiler.symbols.TypeSymbol
import mango.compiler.symbols.VariableSymbol
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

    inline fun extendIfNecessary(value: LLVMValue, type: LLVMType) = if (value.type == type) value else extend(value, type)
    inline fun extend(value: LLVMValue, type: LLVMType) = when (type) {
        is LLVMType.I -> tmpVal(Conversion(Conversion.Kind.SignExt, value, type)).ref
        is LLVMType.U -> tmpVal(Conversion(Conversion.Kind.ZeroExt, value, type)).ref
        is LLVMType.Float -> tmpVal(Conversion(Conversion.Kind.FloatExt, value, type)).ref
        else -> throw LLVMEmitterError("type ${type.code} can't be extended")
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

    fun getPtr(struct: LLVMValue, arrayI: LLVMValue, fieldI: LLVMValue?, type: LLVMType): LocalRef {
        return tmpVal(GetPtr(
            (struct.type as LLVMType.Ptr<*>).element,
            struct,
            arrayI,
            fieldI,
            type = LLVMType.Ptr(type)
        )).ref
    }

    fun getStructField(struct: LLVMValue, i: Int, field: TypeSymbol.StructTypeSymbol.Field) =
        Load(getPtr(struct, LLVMValue.Int(0, LLVMType.I32), LLVMValue.Int(i, LLVMType.I32), LLVMType[field.type]))

    fun setStructField(struct: LLVMValue, i: Int, field: TypeSymbol.StructTypeSymbol.Field, value: LLVMValue) =
        store(getPtr(struct, LLVMValue.Int(0, LLVMType.I32), LLVMValue.Int(i, LLVMType.I32), LLVMType[field.type]), value)

    fun getArrayElement(array: LLVMValue, i: LLVMValue) =
        Load(getPtr(array, i, null, (array.type as LLVMType.Ptr<*>).element))

    fun setArrayElement(array: LLVMValue, i: LLVMValue, value: LLVMValue) =
        store(getPtr(array, i, null, (array.type as LLVMType.Ptr<*>).element), value)

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

    inline fun malloc(type: LLVMType) = mallocArray(type, 1)
    inline fun mallocArray(type: LLVMType, size: Int): LLVMValue {
        val retType = LLVMType.Ptr(LLVMType.I8)
        if (!functionBuilder.moduleBuilder.hasFunctionName("malloc")) {
            functionBuilder.moduleBuilder.addDeclaration(FunctionDeclaration(
                "malloc", retType, listOf(LLVMType.I32)
            ))
        }
        val heap = tmpVal(Call(retType, LLVMValue.GlobalRef("malloc", LLVMType.Fn(retType, listOf(LLVMType.I32))), LLVMValue.Int(type.bits * size, LLVMType.I32))).ref
        return tmpVal(Conversion(Conversion.Kind.BitCast, heap, type)).ref
    }
    inline fun mallocArray(type: LLVMType, size: LLVMValue): LLVMValue {
        val retType = LLVMType.Ptr(LLVMType.I8)
        if (!functionBuilder.moduleBuilder.hasFunctionName("malloc")) {
            functionBuilder.moduleBuilder.addDeclaration(FunctionDeclaration(
                "malloc", retType, listOf(LLVMType.I32)
            ))
        }
        val realSize = operation(Operation.Kind.IntMul, LLVMValue.Int(type.bits, LLVMType.I32), size)
        val heap = tmpVal(Call(retType, LLVMValue.GlobalRef("malloc", LLVMType.Fn(retType, listOf(LLVMType.I32))), realSize)).ref
        return tmpVal(Conversion(Conversion.Kind.BitCast, heap, type)).ref
    }

    inline fun free() {
        val type = LLVMType.Ptr(LLVMType.I8)
        if (!functionBuilder.moduleBuilder.hasFunctionName("free")) {
            functionBuilder.moduleBuilder.addDeclaration(FunctionDeclaration(
                    "free", LLVMType.Void, listOf(type)
            ))
        }
        addInstruction(Call(LLVMType.Void, LLVMValue.GlobalRef("malloc", LLVMType.Fn(LLVMType.Void, listOf(LLVMType.I8))), LLVMValue.Int(type.bits, LLVMType.I8)))
    }

    fun code() = (if (name != null) "$name:\n    " else "") + instructions.joinToString(separator = "\n    ") { it.code }

    inline fun cStringConstForContent(content: String) = functionBuilder.cStringConstForContent(content)
    inline fun stringConstForContent(content: String): GlobalVar = functionBuilder.stringConstForContent(content)

    fun label(): Label {
        if (name == null) throw LLVMEmitterError("Label name can't be null")
        return Label(name)
    }

    inline fun load(value: LLVMValue): LocalRef {
        val tempValue = tmpVal(Load(value, (value.type as LLVMType.Ptr<*>).element))
        return tempValue.ref
    }

    inline fun load(name: String, value: LLVMValue): LocalRef {
        val tempValue = tmpVal(name, Load(value, (value.type as LLVMType.Ptr<*>).element))
        return tempValue.ref
    }

    inline fun store(destination: LLVMValue, value: LLVMValue) {
        addInstruction(Store(value, destination))
    }

    inline fun operation(kind: Operation.Kind, left: LLVMValue, right: LLVMValue): LLVMValue {
        val operation = Operation(kind, left, right)
        return tmpVal(operation).ref
    }
}

class FunctionBuilder(
    val moduleBuilder: ModuleBuilder,
    val type: TypeSymbol.Fn,
    val parameters: Array<VariableSymbol>,
    val mangledName: String
) {
    val returnType = LLVMType[type.returnType]

    val paramTypes = type.args.map { LLVMType[it] }

    private val blocks = LinkedList<BlockBuilder>()
    private val attributes = LinkedList<String>()

    init {
        blocks.add(BlockBuilder(this))
    }

    private var nextTmpIndex = 0
    fun tmpIndex () = nextTmpIndex++

    fun addAttribute (string: String) = attributes.add(string)

    fun code() =
        "define ${returnType.code} @\"$mangledName\"(${paramTypes.map(LLVMType::code).joinToString(separator = ", ")}) ${attributes.joinToString(" ")} {\n" +
        "    ${blocks.joinToString("\n") { it.code() }}\n}\n"


    val entryBlock: BlockBuilder get() = blocks.first

    fun createBlock (name: String?): BlockBuilder {
        val block = BlockBuilder(this, name)
        blocks.add(block)
        return block
    }

    fun cStringConstForContent (content: String) = moduleBuilder.cStringConstForContent(content)
    fun stringConstForContent (content: String) = moduleBuilder.stringConstForContent(content)

    fun paramReference (index: Int): LLVMValue {
        if (index < 0 || index >= paramTypes.size) {
            throw LLVMEmitterError("Expected an index between 0 and ${paramTypes.size - 1}, found $index")
        }
        val type = paramTypes[index]
        return LLVMValue.ParamRef(index, type)
    }
}
