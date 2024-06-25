package com.googlecode.cqengine.persistence.support.serialization;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.annotation.Annotation;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import io.activej.serializer.annotations.Deserialize;
import io.activej.serializer.annotations.Serialize;

class ActiveJSerializerTests {

    @Test void testPositiveSerializability() {
        ActiveJSerializer.validateObjectIsRoundTripSerializable(new SerializablePojo(1));
    }

    @Test void testNegativeSerializability() {      
        assertThrows(IllegalStateException.class, () -> ActiveJSerializer.validateObjectIsRoundTripSerializable(new NonSerializablePojo(1)));    
    }

    @Test void testValidateObjectEquality() {
        assertThrows(IllegalStateException.class, () -> ActiveJSerializer.validateObjectEquality(1, 2));
    }

    @Test void testValidateHashCodeEquality() {
        assertThrows(IllegalStateException.class, () -> ActiveJSerializer.validateHashCodeEquality(1, 2));
    }

    @Test void testPolymorphicSerialization_WithNonPolymorphicConfig() {
        assertThrows(IllegalStateException.class, () -> ActiveJSerializer.validateObjectIsRoundTripSerializable(new MyNumber(1), MyNumber.class, PersistenceConfig.DEFAULT_CONFIG));
    }

    @Test void testPolymorphicSerialization_WithPolymorphicConfig() {
        ActiveJSerializer.validateObjectIsRoundTripSerializable(new MyNumberSerializable(1), MyNumberSerializable.class, POLYMORPHIC_CONFIG);
    }

    @SuppressWarnings("unused")
    public static class SerializablePojo {
        public SerializablePojo(@Deserialize("i") int i) {
            this.i = i;
        }

        @Serialize
        public int i;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SerializablePojo)) return false;
    
            SerializablePojo that = (SerializablePojo) o;
    
            return i == that.i;
        }
    
        @Override
        public int hashCode() {
            return i;
        }
    }

    @SuppressWarnings("unused")
    public static class NonSerializablePojo {
        int i;
        public NonSerializablePojo() {
            throw new IllegalStateException("Intentional exception");
        }
        public NonSerializablePojo(int i) {
            this.i = i;
        }
    }

    @SuppressWarnings("unused")
    public static class MyNumber extends Number {
        private final double value;
    
        public MyNumber(double value) {
            this.value = value;
        }
    
        @Override
        public int intValue() {
            return (int) value;
        }
    
        @Override
        public long longValue() {
            return (long) value;
        }
    
        @Override
        public float floatValue() {
            return (float) value;
        }
    
        @Override
        public double doubleValue() {
            return value;
        }
    
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            MyNumber myNumber = (MyNumber) obj;
            return Double.compare(myNumber.value, value) == 0;
        }
    
        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }

    @SuppressWarnings("unused")
    public static class MyNumberSerializable extends MyNumber {
        
        @Serialize
        public double value;
    
        public MyNumberSerializable(@Deserialize("value") double value) {
            super(value);
            this.value = value;
        }
    }    

    PersistenceConfig POLYMORPHIC_CONFIG = new PersistenceConfig() {

        @Override
        public Class<? extends Annotation> annotationType() {
            return PersistenceConfig.class;
        }

        @Override
        public Class<? extends PojoSerializer> serializer() {
            return ActiveJSerializer.class;
        }

        @Override
        public boolean polymorphic() {
            return true;
        }
    };
}
