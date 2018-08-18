package software.hsharp.core.util

import java.util.*
import java.io.*

open class Ini(protected val fileName: String) {
    protected val prop = Properties()

    fun load() {
        prop.clear()
        var input: InputStream? = null
        val fileFullName = getPath(fileName)
        val f = File(fileFullName)
        try {
            input = FileInputStream(f)

            // load a properties file
            prop.load(input)
        } catch (e: FileNotFoundException) {
            // file is not there, allow to init
            initEmptyFile()
        } catch (ex: IOException) {
            println("Failed to open '$fileName' from '$fileFullName' expected at '${f.absoluteFile}'")
            ex.printStackTrace()
        } finally {
            if (input != null) {
                try {
                    input.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun save() {
        var output: OutputStream? = null

        try {
            val fileName = getPath(fileName)
            val f = File(fileName)
            f.absoluteFile.parentFile.mkdirs()
            output = FileOutputStream(f)

            prop.store(output, null)
        } catch (io: IOException) {
            io.printStackTrace()
        } finally {
            if (output != null) {
                try {
                    output.flush()
                    output.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    protected open fun initEmptyFile() { save(); }

    companion object {
        fun getPath(fileName: String): String {
            return "etc" + File.separator + fileName + ".properties"
        }

        fun load(
            fileName: String,
            setupIni: (String) -> Ini = { Ini(fileName); }
        ): Ini {
            return load(
                fileName,
                setupIni,
                { it.initEmptyFile(); }
            )
        }

        fun load(
            fileName: String,
            setupIni: (String) -> Ini = { Ini(fileName); },
            setupEmptyProperties: (Ini) -> Unit = { it.initEmptyFile(); }
        ): Ini {
            val result = setupIni(fileName)
            val fileFullName = getPath(fileName)
            val f = File(fileFullName)
            if (!f.exists()) {
                // file is not there, allow to init
                setupEmptyProperties(result)
            }
            result.load()

            return result
        }
    }
}