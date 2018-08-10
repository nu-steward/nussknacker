package pl.touk.nussknacker.engine.kafka.serialization

import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.streaming.util.serialization.KeyedDeserializationSchema
import org.apache.kafka.common.serialization.Deserializer
import pl.touk.nussknacker.engine.kafka.KafkaConfig

/**
  * Factory class for Flink's KeyedDeserializationSchema. It is extracted for  purpose when serialization
  * of KeyedDeserializationSchema is hard to achieve.
  *
  * @tparam T type of deserialized object
  */
trait DeserializationSchemaFactory[T] extends Serializable {

  def create(topics: List[String], kafkaConfig: KafkaConfig): KeyedDeserializationSchema[T]

}

/**
  * Factory which always return the same schema.
  *
  * @param deserializationSchema schema which will be returned.
  * @tparam T type of deserialized object
  */
case class FixedDeserializationSchemaFactory[T](deserializationSchema: KeyedDeserializationSchema[T])
  extends DeserializationSchemaFactory[T] {

  override def create(topics: List[String], kafkaConfig: KafkaConfig): KeyedDeserializationSchema[T] = deserializationSchema

}

/**
  * Abstract base implementation of [[pl.touk.nussknacker.engine.kafka.serialization.DeserializationSchemaFactory]]
  * which uses Kafka's Deserializer in returned Flink's KeyedDeserializationSchema. It deserializes only value.
  *
  * @tparam T type of deserialized object
  */
abstract class KafkaDeserializationSchemaFactoryBase[T: TypeInformation]
  extends DeserializationSchemaFactory[T] {

  protected def createValueDeserializer(topics: List[String], kafkaConfig: KafkaConfig): Deserializer[T]

  override def create(topics: List[String], kafkaConfig: KafkaConfig): KeyedDeserializationSchema[T] = {
    val valueDeserializer = createValueDeserializer(topics, kafkaConfig)

    new KeyedDeserializationSchema[T] {

      override def deserialize(messageKey: Array[Byte], message: Array[Byte], topic: String, partition: Int, offset: Long): T = {
        val value = valueDeserializer.deserialize(topic, message)
        value
      }

      override def isEndOfStream(nextElement: T): Boolean = false

      override def getProducedType: TypeInformation[T] = implicitly[TypeInformation[T]]

    }
  }

}

/**
  * Abstract base implementation of [[pl.touk.nussknacker.engine.kafka.serialization.DeserializationSchemaFactory]]
  * which uses Kafka's Deserializer in returned Flink's KeyedDeserializationSchema. It deserializes both key and value
  * and wrap it in object T
  *
  * @tparam T type of deserialized object
  */
abstract class KafkaKeyValueDeserializationSchemaFactoryBase[T: TypeInformation]
  extends DeserializationSchemaFactory[T] {

  protected type K

  protected type V

  protected def createKeyDeserializer(topics: List[String], kafkaConfig: KafkaConfig): Deserializer[K]

  protected def createValueDeserializer(topics: List[String], kafkaConfig: KafkaConfig): Deserializer[V]

  protected def createObject(key: K, value: V, topic: String): T

  override def create(topics: List[String], kafkaConfig: KafkaConfig): KeyedDeserializationSchema[T] = {
    val keyDeserializer = createKeyDeserializer(topics, kafkaConfig)
    val valueDeserializer = createValueDeserializer(topics, kafkaConfig)

    new KeyedDeserializationSchema[T] {

      override def deserialize(messageKey: Array[Byte], message: Array[Byte], topic: String, partition: Int, offset: Long): T = {
        val key = keyDeserializer.deserialize(topic, messageKey)
        val value = valueDeserializer.deserialize(topic, message)
        val obj = createObject(key, value, topic)
        obj
      }

      override def isEndOfStream(nextElement: T): Boolean = false

      override def getProducedType: TypeInformation[T] = implicitly[TypeInformation[T]]

    }
  }

}