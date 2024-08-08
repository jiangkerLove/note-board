import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.jiangker.noteboard.NoteElement
import com.jiangker.noteboard.NoteOperation
import java.lang.reflect.Type

class NoteDeserializer : JsonDeserializer<NoteOperation> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): NoteOperation {
        val jsonObject = json.asJsonObject
        return when (val type = jsonObject.get("t").asString) {
            "Add" -> {
                context.deserialize<NoteOperation.AddElement>(json, NoteOperation.AddElement::class.java)
            }

            "Remove" -> {
                context.deserialize<NoteOperation.RemoveElement>(json, NoteOperation.RemoveElement::class.java)
            }

            "Clean" -> {
                context.deserialize<NoteOperation.CleanElement>(json, NoteOperation.CleanElement::class.java)
            }

            else -> throw IllegalArgumentException(type)
        }
    }
}

class ElementDeserializer : JsonDeserializer<NoteElement> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): NoteElement {
        val jsonObject = json.asJsonObject
        return when (val type = jsonObject.get("t").asString) {
            "Line" -> {
                context.deserialize<NoteElement.Line>(json, NoteElement.Line::class.java)
            }

            else -> throw IllegalArgumentException(type)
        }
    }
}