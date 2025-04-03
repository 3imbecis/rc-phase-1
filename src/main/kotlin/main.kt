import java.io.IOException
import java.net.Socket
import java.util.Scanner

fun main(){

    println()
    println(" ----- TERMINAL WEB CLIENT ----- ")

    while(true){

        val url = requestUrl()

        println()
        println(" Connecting to ${if(url.port == 80) url.host else "${url.host}:${url.port}"}...")

        var client: Socket
        try{
            client = Socket(url.host, url.port)
            println(" Connected!")
            println()
        }catch(e: IOException){
            println(" Failed to connect: ${e.message}")
            continue;
        }

        println(" Sending HTTP request...")
        val request = listOf(
            "GET /${url.path} HTTP/1.1",
            "Host: ${url.host}",
            "User-Agent: Terminal-Web-Client/1.0",
            "Accept: */*",
            "Connection: close",
            ""
        ).joinToString("\r\n") + "\r\n"
        printMessage("HTTP Request", request);

        client.getOutputStream().write(request.toByteArray())
        // print

        println(" Waiting for the HTTP response...")

        val scanner = Scanner(client.getInputStream())

        var response = mutableListOf<String>()

        do{

            if(scanner.hasNextLine())
                response.add(scanner.nextLine())

        }while(response.last() != "")
        printMessage("HTTP Response", response.joinToString("\n"))

        println(" Done response!")

    }

}

data class URL(val host: String, val port: Int, val path: String)

fun requestUrl(): URL {

    println()
    print(" URL: http://")
    val url = readln().trim()

    if(url.isEmpty()){
        println(" A URL is required!")
        return requestUrl()
    }

    val urlParts = url.split('/', limit = 2)
    val host = urlParts[0]
    if(host.isEmpty()){
        println(" The host is required!")
        return requestUrl()
    }
    val path = if(urlParts.size > 1) urlParts[1] else ""

    val hostParts = host.split(':', limit = 2)
    val hostname = hostParts[0];
    if(hostname.isEmpty()){
        println(" The host name is required!")
        return requestUrl()
    }
    val port = (if(hostParts.size > 1) hostParts[1] else "80").toIntOrNull()
    if(port == null){
        println(" Failed to parse the port number!")
        return requestUrl()
    }
    if(port !in 0..65535){
        println(" The port must be between 0 and 65535! (inclusive)");
        return requestUrl()
    }

    return URL(hostname, port, path)
}

fun printMessage(title: String, message: String){

    val lines = message
        .trim()
        .replace("\r", "")
        .split('\n')
    val width = lines.maxOfOrNull { it.length } ?: 0

    println()
    println("┌─[ $title ]${"─".repeat(width - title.length - 4)}─┐")
    println("│ ${" ".repeat(width)} │")
    for(line in lines){
        println("│ ${line}${" ".repeat(width - line.length)} │")
    }
    println("│ ${" ".repeat(width)} │")
    println("└─${"─".repeat(width)}─┘")
    println()

}