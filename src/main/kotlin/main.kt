import java.io.IOException
import java.net.Socket
import java.util.Scanner
import kotlin.math.min

const val RED = "\u001b[31m"
const val GREEN = "\u001b[92m"
const val GRAY = "\u001b[90m"
const val BLUE = "\u001b[94m"
const val YELLOW = "\u001b[33m"
const val BLUE_BG = "\u001b[104m\u001b[97m"
const val RESET = "\u001b[0m"

fun main(){

    println()
    println(" ${GRAY}----- ${BLUE}TERMINAL WEB CLIENT ${GRAY}-----${RESET} ")

    while(true){

        // Ask for a URL
        val url = requestUrl()

        // Connect to the server

        println()
        println(" Connecting to ${if(url.port == 80) url.host else "${url.host}:${url.port}"}...")

        var client: Socket
        try{
            client = Socket(url.host, url.port)
            println(" ${GREEN}Connected!${RESET}")
        }catch(e: IOException){
            println(" ${RED}Failed to connect! ${GRAY}${e.message}${RESET}")
            continue;
        }

        val scanner = Scanner(client.getInputStream())

        // Send HTTP request

        println("\n Sending HTTP request...\n")
        val request = listOf(
            "GET /${url.path} HTTP/1.1",
            "Host: ${url.host}",
            "User-Agent: Terminal-Web-Client/1.0",
            "Accept: */*",
            "Connection: close",
            ""
        ).joinToString("\r\n") + "\r\n"
        prettyPrint("HTTP Request", request);

        client.getOutputStream().write(request.toByteArray())

        // Receive HTTP response headers

        println("\n Waiting for the HTTP response headers...\n")

        var responseHeaders = mutableListOf<String>()
        do{

            if(scanner.hasNextLine())
                responseHeaders.add(scanner.nextLine())

        }while(responseHeaders.last() != "")

        prettyPrint("HTTP Response Headers", responseHeaders.joinToString("\n"))

        codeDecoder(responseHeaders)

        // TODO: Handle headers and if 200 or wtv, recieve content
    }

}

data class URL(val host: String, val port: Int, val path: String)

fun requestUrl(): URL {

    println()
    print(" ${BLUE}URL:${RESET} http://")
    val url = readln().trim()

    if(url.isEmpty()){
        println(" ${YELLOW}A URL is required!${RESET}")
        return requestUrl()
    }

    val urlParts = url.split('/', limit = 2)
    val host = urlParts[0]
    if(host.isEmpty()){
        println(" ${YELLOW}The host is required!${RESET}")
        return requestUrl()
    }
    val path = if(urlParts.size > 1) urlParts[1] else ""

    val hostParts = host.split(':', limit = 2)
    val hostname = hostParts[0];
    if(hostname.isEmpty()){
        println(" ${YELLOW}The host name is required!${RESET}")
        return requestUrl()
    }
    val port = (if(hostParts.size > 1) hostParts[1] else "80").toIntOrNull()
    if(port == null){
        println(" ${YELLOW}Failed to parse the port number!${RESET}")
        return requestUrl()
    }
    if(port !in 0..65535){
        println(" ${YELLOW}The port must be between 0 and 65535! (inclusive)${RESET}");
        return requestUrl()
    }

    return URL(hostname, port, path)
}

const val MAX_WIDTH = 70

fun prettyPrint(title: String, message: String){

    val lines = message
        .trim()
        .replace("\r", "")
        .split('\n')
    val width = (lines.maxOfOrNull { it.length } ?: 0).coerceIn((title.length + 2)..MAX_WIDTH)

    println("${BLUE}┌─${RESET + BLUE_BG} $title ${RESET + BLUE + "─".repeat(width - title.length - 2)}─┐${RESET}")
    println("${BLUE}│ ${" ".repeat(width)} │${RESET}")
    for(line in lines){
        print("${BLUE}│${RESET} ")
        print(if(line.length < 100) line else "${line.substring(0..<(MAX_WIDTH - 3))}$GRAY...$RESET")
        print(" ".repeat(width - min(line.length, MAX_WIDTH)))
        println(" ${BLUE}│${RESET}")
    }
    println("${BLUE}│ ${" ".repeat(width)} │${RESET}")
    println("${BLUE}└─${"─".repeat(width)}─┘${RESET}")

}

fun parseHeaders(lines: List<String>){

    if(lines.isEmpty()){
        println(" ${RED}Invalid response: No headers were sent!${RESET}")
    }

    // TODO: finish this

}

fun codeDecoder(array: List<String>){

    var code = ""

    for(i in array.indices){
        code = array[0]
    }

    val codeInt = code.split(" ")[1].toInt()

    val method100 = listOf(
        "Your code was $codeInt",
        "This type of response indicates that the request",
        "was received and understood",
        "The 100 codes goes from 100 to 104",
        ).joinToString("\r\n") + "\r\n"

    val method200 = listOf(
        "Your code was $codeInt",
        "This class of status codes indicates the action requested by the",
        "client was received, understood, and accepted",
        "The 200 codes goes from 200 to 226",
    ).joinToString("\r\n") + "\r\n"

    val method300 = listOf(
        "Your code was $codeInt",
        "This class of status code indicates the client must take additional",
        "action to complete the request",
        "Many of these status codes are used in URL redirection",
        "The 300 codes goes from 300 to 308",
    ).joinToString("\r\n") + "\r\n"

    val method400 = listOf(
        "Your code was $codeInt",
        "This class of status code is intended for situations in which the",
        "error seems to have been caused by the client",
        "These status codes are applicable to any request method",
        "The 400 codes goes from 400 to 451",
    ).joinToString("\r\n") + "\r\n"

    val method500 = listOf(
        "Your code was $codeInt",
        "The 500 codes indicates that it has encountered an error or is",
        "otherwise incapable of performing the request",
        "The 500 codes goes from 500 to 511",
    ).joinToString("\r\n") + "\r\n"

    when(codeInt){
        in 100 .. 200 -> prettyPrint("100 Code Description",method100)
        in 200 .. 300 -> prettyPrint("200 Code Description",method200)
        in 300 .. 400 -> prettyPrint("300 Code Description",method300)
        in 400 .. 500 -> prettyPrint("400 Code Description",method400)
        in 500 .. 600 -> prettyPrint("500 Code Description",method500)
    }

}