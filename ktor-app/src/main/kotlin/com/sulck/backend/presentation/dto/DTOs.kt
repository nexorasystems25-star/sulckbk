package com.sulck.backend.presentation.dto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.UUID
import java.time.Instant

// UUID and Instant serializers using kotlinx-serialization built-in
val UUID_serializer: KSerializer<UUID> = kotlinx.serialization.serializers.builtInSerializers().serializer(java.type.UUID::class.java)
val Instant_serializer: KSerializer<Instant> = kotlinx.serialization.serializers.builtInSerializers().serializer(java.time.Instant::class.java)

// Helper for serializing Lists of serializable types
fun <T : Serializable> listSerializer(of: KSerializer<T>): KSerializer<List<T>> {
    return kotlinx.serialization.serializers.ListSerializer(of)
}