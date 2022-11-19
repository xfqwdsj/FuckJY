import java.io.File
import java.util.*

fun elevatedProcess(
    program: String,
    args: String,
    tmpFile: File = File.createTempFile("fuckjy-temp-${UUID.randomUUID()}", ".tmp.vbs")
): ProcessBuilder {
    tmpFile.deleteOnExit()
    tmpFile.writeText(
        "Set objShell = CreateObject(\"Shell.Application\")\n" +
                "objShell.ShellExecute \"${program}\", \"${args}\", \"\", \"runas\", 0"
    )
    tmpFile.setExecutable(true)
    return ProcessBuilder("CScript", tmpFile.absolutePath)
}