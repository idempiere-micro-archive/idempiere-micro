package software.hsharp.core.util

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeType

fun parse(body: String): MutableList<Pair<String, Any>> {
    if (body == "") {
        return mutableListOf()
    }

    val factory = JsonFactory()

    val mapper = ObjectMapper(factory)
    val rootNode = mapper.readTree(body)

    val fieldsIterator = rootNode.fields()
    val values: MutableList<Pair<String, Any>> = mutableListOf()
    while (fieldsIterator.hasNext()) {

        val field = fieldsIterator.next()
        val nodeValue: JsonNode = field.value

        val value: Any =
                when (nodeValue.nodeType) {
                    JsonNodeType.STRING -> nodeValue.asText()
                    JsonNodeType.NUMBER ->
                        if (nodeValue.toString().contains(".")) {
                            nodeValue.asDouble()
                        } else {
                            nodeValue.asInt()
                        }
                    else -> {
                        nodeValue
                    }
                }

        val mappedField: Pair<String, Any> = Pair(field.key, value)
        values.add(mappedField)
    }
    return values
}
