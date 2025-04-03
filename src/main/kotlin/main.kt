import java.io.IOException
import java.net.Socket
import java.util.Scanner
import kotlin.math.min

const val RED = "\u001b[31m"
const val RED_BG = "\u001b[41m\u001b[97m"
const val GREEN = "\u001b[92m"
const val GREEN_BG = "\u001b[102m\u001b[97m"
const val GRAY = "\u001b[90m"
const val BLUE = "\u001b[94m"
const val BLUE_BG = "\u001b[104m\u001b[97m"
const val YELLOW = "\u001b[33m"
const val YELLOW_BG = "\u001b[43m\u001b[97m"
const val RESET = "\u001b[0m"

enum class Color { BLUE, RED, YELLOW, GREEN }

val EXPLANATION = mapOf(
    /* Successful */
    200 to Pair("Everything worked as intended!", Color.GREEN),
    201 to Pair("Something was successfully created as per your request.", Color.GREEN),
    204 to Pair("The response doesn't contain anything other than the headers.", Color.GREEN),
    /* Redirection */
    301 to Pair("The requested resource moved permanently to another path.", Color.BLUE),
    302 to Pair("The requested resource moved temporarily on another path.", Color.BLUE),
    304 to Pair("Nothing changed since the last time you requested this.", Color.BLUE),
    307 to Pair("The requested resource moved temporarily on another path.", Color.BLUE),
    308 to Pair("The requested resource moved permanently to another path.", Color.BLUE),
    /* Client Error */
    400 to Pair("The server can't or won't process your request.", Color.YELLOW),
    401 to Pair("You need to authenticate to access this resource.", Color.YELLOW),
    402 to Pair("You need to pay to access this resource.", Color.YELLOW),
    403 to Pair("You don't have access to this resource.", Color.YELLOW),
    404 to Pair("The requested resource wasn't found on this path.", Color.YELLOW),
    408 to Pair("You took too long to request.", Color.YELLOW),
    410 to Pair("The requested resource was deleted.", Color.YELLOW),
    411 to Pair("You need to specify the length of your uploaded content.", Color.YELLOW),
    413 to Pair("Your uploaded content is too big.", Color.YELLOW),
    414 to Pair("The URL is too long.", Color.YELLOW),
    415 to Pair("The server doesn't support this type of media.", Color.YELLOW),
    418 to Pair("I'm a teapot.", Color.GREEN),
    429 to Pair("You've sent too many requests. You're being rate limited.", Color.YELLOW),
    431 to Pair("One of your headers is too big.", Color.YELLOW),
    451 to Pair("For legal reasons, this resource cannot be displayed.", Color.YELLOW),
    /* Server Error */
    500 to Pair("Something went wrong! Generic server error.", Color.RED)
)

var debug = false
fun debugDelay(sec: Long = 1){
    if(debug) Thread.sleep(sec * 1000)
}

fun main(args: Array<String>){

    if("-debug" in args){
        println("${YELLOW_BG} Debug mode enabled ${RESET}")
        debug = true
    }

    println()
    println(" ${GRAY}----- ${BLUE}TERMINAL WEB CLIENT ${GRAY}-----${RESET} ")

    while(true){

        var url: URL?
        do{
            print("\n ${BLUE}URL:${RESET} http://")
            url = parseURL(readln().trim())
        }while(url == null)

        fetch(url)

    }

}

data class URL(
    val host: String,
    val port: Int,
    val path: String
)

fun parseURL(url: String, printError: Boolean = true): URL? {

    if(url.isEmpty()){
        if(printError) println(" ${YELLOW}A URL is required!${RESET}")
        return null
    }

    // Split host and path

    val urlParts = url.split('/', limit = 2)
    val host = urlParts[0]
    if(host.isEmpty()){
        if(printError) println(" ${YELLOW}The host is required!${RESET}")
        return null
    }
    val path = if(urlParts.size > 1) urlParts[1] else ""

    // Split hostname and port

    val hostParts = host.split(':', limit = 2)
    val hostname = hostParts[0];
    if(hostname.isEmpty()){
        if(printError) println(" ${YELLOW}The host name is required!${RESET}")
        return null
    }
    val port = (if(hostParts.size > 1) hostParts[1] else "80").toIntOrNull()
    if(port == null){
        if(printError) println(" ${YELLOW}Failed to parse the port number!${RESET}")
        return null
    }
    if(port !in 0..65535){
        if(printError) println(" ${YELLOW}The port must be between 0 and 65535! (inclusive)${RESET}");
        return null
    }

    return URL(hostname, port, path)
}

const val MAX_WIDTH = 70

fun prettyPrint(title: String, message: String, color: Color = Color.BLUE){

    val fg = when(color){
        Color.BLUE -> BLUE
        Color.RED -> RED
        Color.YELLOW -> YELLOW
        Color.GREEN -> GREEN
    }
    val bg = when(color){
        Color.BLUE -> BLUE_BG
        Color.RED -> RED_BG
        Color.YELLOW -> YELLOW_BG
        Color.GREEN -> GREEN_BG
    }

    var lines = message
        .trim()
        .replace("\r", "")
        .split('\n')

    if(lines.isEmpty()) lines = listOf<String>("${GRAY}(Empty)${RESET}")

    // calculate box width
    val width = (lines.maxOfOrNull { it.length } ?: 0).coerceIn((title.length + 2)..MAX_WIDTH)

    // top border
    println("${fg}┌─${RESET + bg} $title ${RESET + fg + "─".repeat(width - title.length - 2)}─┐${RESET}")

    // padding
    println("${fg}│ ${" ".repeat(width)} │${RESET}")

    // lines
    for(line in lines){
        // left border
        print("${fg}│${RESET} ")
        // text
        print(
            if(line.length < MAX_WIDTH) line
            else "${line.substring(0..<(MAX_WIDTH - 3))}$GRAY...$RESET"
        )
        // padding
        print(" ".repeat(width - min(line.length, MAX_WIDTH)))
        // right border
        println(" ${fg}│${RESET}")
    }

    // padding
    println("${fg}│ ${" ".repeat(width)} │${RESET}")

    // bottom border
    println("${fg}└─${"─".repeat(width)}─┘${RESET}")

}

data class Headers(
    val status: Int,
    val statusMessage: String,
    val contentLength: Int,
    val chunked: Boolean,
    val original: Map<String, String>
)

fun parseHeaders(lines: List<String>): Headers? {

    if(lines.isEmpty()){
        println(" ${RED}Invalid response: No headers were sent!${RESET}")
        return null
    }

    if(!lines[0].startsWith("HTTP/")){
        println(" ${RED}Invalid response: Not an HTTP response!${RESET}")
        return null
    }

    val (_, statusStr, statusMessage) = lines[0].split(' ', limit = 3)

    val status = statusStr.toIntOrNull()
    if(status == null) {
        println(" ${RED}Invalid status code!${RESET}")
        return null
    }
    if(
        status !in 100..103 &&
        status !in 200..208 && status != 226 &&
        status !in 300..308 &&
        status !in 400..418 && status !in 421..426 && status !in 428..429 && status != 431 && status != 451 &&
        status !in 500..508 && status !in 510..511
    ) println(" ${YELLOW}Unknown status code.${RESET}")

    val headers = lines.mapNotNull { line ->
        val parts = line.split(':', limit = 2)
        if(parts.size == 2)
            parts[0].trim().lowercase() to parts[1].trim()
        else null
    }.toMap()

    return Headers(
        status = status,
        statusMessage = statusMessage,
        contentLength = (headers["content-length"] ?: "0").toIntOrNull() ?: 0,
        chunked = (headers["transfer-encoding"] ?: "") == "chunked",
        original = headers
    )

}

fun fetch(url: URL){

    // Connect to the server

    println()
    println(" Connecting to ${if(url.port == 80) url.host else "${url.host}:${url.port}"}...")
    debugDelay()

    val client: Socket
    try{
        client = Socket(url.host, url.port)
        println(" ${GREEN}Connected!${RESET}")
    }catch(e: IOException){
        println(" ${RED}Failed to connect! ${GRAY}${e.message}${RESET}")
        return
    }
    debugDelay(2)

    val scanner = Scanner(client.getInputStream())

    // Send HTTP request

    println("\n Sending HTTP request...\n")
    debugDelay()

    val request = listOf(
        "GET /${url.path} HTTP/1.1",
        "Host: ${url.host}",
        "User-Agent: Terminal-Web-Client/1.0",
        "Accept: */*",
        "Connection: close",
        ""
    ).joinToString("\r\n") + "\r\n"

    client.getOutputStream().write(request.toByteArray())
    prettyPrint("HTTP Request", request)
    debugDelay(2)

    // Receive HTTP response headers

    println("\n Waiting for the HTTP response headers...\n")
    debugDelay()

    val responseHeaders = mutableListOf<String>()
    do{

        if(scanner.hasNextLine())
            responseHeaders.add(scanner.nextLine())

    }while(responseHeaders.last() != "")

    prettyPrint("HTTP Response Headers", responseHeaders.joinToString("\n"))
    debugDelay(3)

    val headers = parseHeaders(responseHeaders)
    if(headers == null){
        client.close()
        return
    }

    // Explain status code

    val explanation = EXPLANATION[headers.status]
    if(explanation != null){
        prettyPrint(
            "${headers.status} ${headers.statusMessage}: Explanation",
            explanation.first, explanation.second
        )
        debugDelay(3)
    }

    // Check for redirection

    val location = headers.original["location"]
    if(location != null){

        if(!location.startsWith("http://")){
            println("\n ${YELLOW}The redirect URL is unsupported!${RESET}")
            debugDelay()
        }else{
            val redirectURL = parseURL(location.removePrefix("http://"), false)
            if(redirectURL == null) {
                println("\n ${RED}The redirect URL failed to parse!${RESET}")
                debugDelay()
            }else{
                println("\n Redirecting to ${location}...")
                debugDelay()
                client.close()
                return fetch(redirectURL)
            }
        }

    }

    // Wait for content

    if(headers.contentLength > 0){

        println("\n Receiving the HTTP response body...\n")
        debugDelay()

        var content = ""

        while(content.length < headers.contentLength && scanner.hasNextLine()){
            content += "${scanner.nextLine()}\n"
        }

        prettyPrint("HTTP Response Body", content)
        debugDelay(3)

    }else if(headers.chunked){

        println("\n Receiving chunked HTTP response body...\n")
        debugDelay()

        var content = ""


        while(scanner.hasNextLine()){

            val chunkSize = scanner.nextLine().toIntOrNull(16) ?: 0
            if(chunkSize == 0) break;

            repeat(chunkSize){
                if(scanner.hasNextLine())
                    content += "${scanner.nextLine()}\n"
            }
            if(scanner.hasNextLine()) scanner.nextLine() // Consume empty line

        }

        prettyPrint("HTTP Response Body", content)
        debugDelay(3)

    }

}