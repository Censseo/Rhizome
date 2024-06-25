package com.googlecode.cqengine.persistence.support.serialization;

import io.activej.bytebuf.ByteBuf;
import io.activej.bytebuf.ByteBufPool;
import io.activej.serializer.BinarySerializer;
import io.activej.serializer.SerializerBuilder;

/**
 * A serializer which uses ActiveJ to serialize and deserialize objects.
 * Pojos must be annotated with {@link io.activej.serializer.annotations.Serialize} for fields to be serialized. Fields must be public.
 * Constructors must be annotated with {@link io.activej.serializer.annotations.Deserialize}.
 * For example:
 * <pre>
 * 	public static class Person {
		public Person(@Deserialize("age") int age,
				@Deserialize("name") String name) {
			this.age = age;
			this.name = name;
		}

		@Serialize
		public final int age;

		@Serialize
		public final String name;

		private String surname;

		@Serialize
		public String getSurname() {
			return surname;
		}

		public void setSurname(String surname) {
			this.surname = surname;
		}
	}
    * </pre>
 */
public class ActiveJSerializer<O> implements PojoSerializer<O> {

    protected final Class<O> objectType;
    protected final boolean polymorphic;
    protected final ThreadLocal<BinarySerializer<O>> binarySerializerCache;

    public ActiveJSerializer(Class<O> objectType, PersistenceConfig persistenceConfig) {
        this.objectType = objectType;
        this.polymorphic = persistenceConfig.polymorphic();

        this.binarySerializerCache = ThreadLocal.withInitial(() -> createBinarySerializer(objectType));
    }

    /**
     * Creates a new instance of ActiveJ serializer, for use with the given object type.
     * <p/>
     * Note: this method is public to allow end-users to validate compatibility of their POJOs,
     * with the ActiveJ serializer as used by CQEngine.
     *
     * @param objectType The type of object which the instance of ActiveJ will serialize
     * @return a new instance of ActiveJ serializer
     */
    @SuppressWarnings({"ArraysAsListWithZeroOrOneArgument", "WeakerAccess"})
    protected BinarySerializer<O> createBinarySerializer(Class<O> objectType) {
        return SerializerBuilder.create().build(objectType);
    }

    @Override
    public byte[] serialize(O object) {
        if (object == null) {
            throw new NullPointerException("Object was null");
        }

        BinarySerializer<O> serializer = binarySerializerCache.get();
        ByteBuf byteBuf = ByteBufPool.allocate(200);
        try {
            int dataSize = serializer.encode(byteBuf.array(), 0, object);
            byte[] buffer = new byte[dataSize];
            System.arraycopy(byteBuf.array(), 0, buffer, 0, dataSize);
    
            return buffer;
        } finally {
            byteBuf.recycle();
        }
    }

    @Override
    public O deserialize(byte[] bytes) {
        BinarySerializer<O> serializer = binarySerializerCache.get();
        return serializer.decode(bytes, 0);
    }
    
    /**
     * Performs sanity tests on the given POJO object, to check if it can be serialized and deserialized with ActiveJ
     * serialzier as used by CQEngine.
     * <p/>
     * If a POJO fails this test, then it typically means CQEngine will be unable to serialize or deserialize
     * it, and thus the POJO can't be used with CQEngine's off-heap or disk indexes or persistence.
     * <p/>
     * Failing the test typically means the data structures or data types within the POJO are too complex. Simplifying
     * the POJO will usually improve compatibility.
     * <p/>
     * This method will return normally if the POJO passes the tests, or will throw an exception if it fails.
     *
     * @param candidatePojo The POJO to test
     */
    @SuppressWarnings("unchecked")
    public static <O> void validateObjectIsRoundTripSerializable(O candidatePojo) {
        Class<O> objectType = (Class<O>) candidatePojo.getClass();
        ActiveJSerializer.validateObjectIsRoundTripSerializable(candidatePojo, objectType, PersistenceConfig.DEFAULT_CONFIG);
    }

    static <O> void validateObjectIsRoundTripSerializable(O candidatePojo, Class<O> objectType, PersistenceConfig persistenceConfig) {
        try {
            ActiveJSerializer<O> serializer = new ActiveJSerializer<>(
                    objectType,
                    persistenceConfig
            );
            byte[] serialized = serializer.serialize(candidatePojo);
            O deserializedPojo = serializer.deserialize(serialized);
            serializer.binarySerializerCache.remove();  // clear cached ActiveJSerializer instance
            validateObjectEquality(candidatePojo, deserializedPojo);
            validateHashCodeEquality(candidatePojo, deserializedPojo);
        }
        catch (Exception e) {
            throw new IllegalStateException("POJO object failed round trip serialization-deserialization test, object type: " + objectType + ", object: " + candidatePojo, e);
        }
    }

    static void validateObjectEquality(Object candidate, Object deserializedPojo) {
        if (!(deserializedPojo.equals(candidate))) {
            throw new IllegalStateException("The POJO after round trip serialization is not equal to the original POJO");
        }
    }

    static void validateHashCodeEquality(Object candidate, Object deserializedPojo) {
        if (deserializedPojo.hashCode() != candidate.hashCode()) {
            throw new IllegalStateException("The POJO's hashCode after round trip serialization differs from its original hashCode");
        }
    }
}
