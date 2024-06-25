package rhizome.core.serialization;

import org.json.JSONObject;
import java.util.function.Function;

public interface Serializable<T, U> {

   T serialize(U object);

   static <T> T serialize(JSONObject json, Function<JSONObject, T> serializer) {
      return serializer.apply(json);
   }

   U deserialize(T object);

   static <U> U deserialize(JSONObject json, Function<JSONObject, U> deserializer) {
      return deserializer.apply(json);
   }

   U fromJson(JSONObject json);

   static <U> U fromJson(JSONObject json, Function<JSONObject, U> deserializer) {
      return deserializer.apply(json);
   }

   JSONObject toJson(U object);

   static <U> JSONObject toJson(U object, Function<U, JSONObject> serializer) {
      return serializer.apply(object);
   }

}
