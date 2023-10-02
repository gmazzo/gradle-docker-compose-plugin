package io.github.gmazzo.docker.compose

import java.io.IOException
import java.io.OutputStream

internal class TeeOutputStream(
    private val out: OutputStream,
    private val tee: OutputStream,
) : OutputStream() {

    @Throws(IOException::class)
    override fun write(b: Int) {
        out.write(b)
        tee.write(b)
    }

    @Throws(IOException::class)
    override fun write(b: ByteArray) {
        out.write(b)
        tee.write(b)
    }

    @Throws(IOException::class)
    override fun write(b: ByteArray, off: Int, len: Int) {
        out.write(b, off, len)
        tee.write(b, off, len)
    }

    @Throws(IOException::class)
    override fun flush() {
        out.flush()
        tee.flush()
    }

    @Throws(IOException::class)
    override fun close() = try {
        out.close()

    } finally {
        tee.close()
    }
}
