package fibo;

import java.math.BigInteger;

/**
 */
public class ClassicFibonacci {

    BigInteger calc(BigInteger value) {
        if (value.compareTo(BigInteger.ONE) <= 0) {
            return value;
        } else {
            return calc(value.subtract(BigInteger.ONE)).add(
                    calc(value.subtract(BigInteger.ONE).subtract(BigInteger.ONE)));
        }
    }
}
