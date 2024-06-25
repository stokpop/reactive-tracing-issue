package nl.stokpop.performance.reactive

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.ArrayList
import java.util.Collections
import java.util.function.BiConsumer
import java.util.function.BinaryOperator
import java.util.function.Function
import java.util.function.Supplier
import java.util.stream.Collector

class ByteBufferToStringCollector : Collector<MutableList<ByteBuffer>, MutableList<ByteBuffer>, String> {

    override fun supplier(): Supplier<MutableList<ByteBuffer>> {
        return Supplier { ArrayList() }
    }

    override fun accumulator(): BiConsumer<MutableList<ByteBuffer>, MutableList<ByteBuffer>> {
        return BiConsumer { holder, next -> holder.addAll(next) }
    }

    override fun combiner(): BinaryOperator<MutableList<ByteBuffer>> {
        return BinaryOperator { s1, s2 ->
            s1.addAll(s2)
            s1
        }
    }

    override fun finisher(): Function<MutableList<ByteBuffer>, String> {
        return Function { list ->
            val builder = StringBuilder()
            for (byteBuffer in list) {
                builder.append(StandardCharsets.UTF_8.decode(byteBuffer))
            }
            builder.toString()
        }
    }

    override fun characteristics(): Set<Collector.Characteristics> {
        return Collections.emptySet()
    }
}
