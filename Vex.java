import javax.xml.bind.DatatypeConverter;
import java.util.ArrayList;

/**
 * Vex: a library for encoding vectors
 * 
 * @author Nathan Walker, Rhythm Labs LLC
 * @version 1.0.0, 01/17/15
 * 
 */

public class Vex {
	
	/**
	 * The header byte for each encoding type
	 */
	public static final byte ENCODING_V1 = (byte) 0x10;
	public static final byte ENCODING_V1_COMPRESSED = (byte) 0x1C;
	
	/**
	 * Encodes the vector using the latest version of the encoding system
	 * @param sequence	the int array that represents the numerical vector
	 * @return the raw-byte array for storage
	 */ 
	public static byte[] encode(int[] sequence) {
		
		// A byte array of the maximum possible needed size
		// Calculated assuming that each int is > 1 byte,
		// with an extra header byte
		byte[] worstCase = new byte[sequence.length * 3 + 1];
		
		// Add the header byte
		worstCase[0] = ENCODING_V1;
		
		// The current index in the output array
		// Starts at 1 because the first byte is the header byte
		int index = 1;
		
		// For each number in the int, sequence:
		// add it to the array and update the most recent index
		for (int i = 0; i < sequence.length; i++) {
			index = insertBytesIntoArray(numberToBytes(sequence[i]), worstCase, index);
		}
		
		// create a new output array of the actual needed size
		byte[] out = new byte[index];
		
		// Copy the elements from the worst case array to the actual array
		for (int i = 0; i < out.length; i++) {
			out[i] = worstCase[i];
		}
		
		return out;
	}
	
	/**
	 * Encodes the vector using the latest version of the encoding system, with compression
	 * @param sequence	the int array that represents the numerical vector
	 * @return the raw-byte array for storage
	 */
	public static byte[] encodeCompressed(int[] sequence) {
		
		// A byte array of the maximum possible needed size
		// Calculated assuming that each int is > 1 byte,
		// with an extra header byte
		byte[] worstCase = new byte[sequence.length * 3 + 1];
		
		// Add the header byte
		worstCase[0] = ENCODING_V1_COMPRESSED;
		
		// The current index in the output array
		// Starts at 1 because the first byte is the header byte
		int index = 1;
		
		// For each int in the sequence:
		for (int i = 0; i < sequence.length; i++) {
			if (sequence[i] == 0) {
				
				// If the int is 0, seek forward to see how many continuous zeros there are
				
				int zeroCount = 0;
				while (i < sequence.length && sequence[i] == 0 && zeroCount < 65536) {
					zeroCount += 1;
					i++;
				}
				
				// Set the index in the int sequence back to the proper position after
				// the while loop
				i--;
				
				// If there are more than two zeros:
				if (zeroCount > 2) {
					
					// Three bytes out:
					byte[] out = new byte[3];
					
					// First byte: a flag character designating a sequence of zeros
					out[0] = (byte) 0xFF;
					
					// Encode the zeroCount to bytes using the standard system
					byte[] zeroCountBytes = numberToBytes(zeroCount);
					
					if (zeroCountBytes.length == 1) {
						// If the count is < 254, pad it with a zero
						// Two bytes are expected to denote the number of zeros
						out[1] = (byte) 0x00;
						out[2] = zeroCountBytes[0];
					} else if (zeroCountBytes.length == 3) {
						// Copy the bytes directly into the output
						out[1] = zeroCountBytes[1];
						out[2] = zeroCountBytes[2];
					}
					
					// Insert the bytes into the output
					index = insertBytesIntoArray(out, worstCase, index);
				} else {
					// If there are only one or two zeros, just insert them
					// It is more space efficient this way
					byte[] out = new byte[zeroCount];
					
					index = insertBytesIntoArray(out, worstCase, index);
				}
				
			} else {
				
				// Insert the number into the byte array, after encoding it properly
				index = insertBytesIntoArray(numberToBytes(sequence[i]), worstCase, index);
			}
		}
		
		// Create a new byte array of the exact length needed.
		byte[] out = new byte[index];
		
		// Copy from the worst case into the new array
		for (int i = 0; i < out.length; i++) {
			out[i] = worstCase[i];
		}
		
		return out;
	}
	
	/**
	 * Encodes the integer into a series of bytes
	 * Only supports numbers up to 16-bits unsigned, < 65536
	 *
	 * @param number	the int to be encoded into the byte format
	 * @return a sequence of one or three bytes representing the int
	 */
	protected static byte[] numberToBytes(int number) {
		byte[] out;
		
		// Checking if the number is larger than 1 byte
		// 254 (0xFE) and 255 (0xFF) are reserved for flag bytes
		if (number > 253) {
			number = shortCeiling(number);
			
			// If number requires 16-bit, output three bytes
			out = new byte[3];
			
			// First byte: flag byte
			out[0] = (byte) 0xfe;
			
			// Second byte: high byte of the number
			out[1] = (byte) ((number & 0xffff) >>> 8);
			
			// Third byte: low byte of the number
			out[2] = (byte) (number & 0xff);
		} else {
			// Just output the byte-sized number, wrapped in an array
			out = new byte[1];
			out[0] = (byte) number;
		}
		
		return out;
	}
	
	/**
	 * Inserts an array of bytes into another array of bytes, starting at a particular index
	 * @param bytes			the array of bytes to be inserted
	 * @param array			the array of bytes into which the bytes will be inserted
	 * @param startIndex	the index to start inserting bytes at
	 * @return the next open index to insert bytes
	 */
	protected static int insertBytesIntoArray(byte[] bytes, byte[] array, int startIndex) {
		for (int i = 0; i < bytes.length; i++) {
			array[startIndex] = bytes[i];
			startIndex++;
		}
		
		return startIndex;
	}
	
	/**
	 * Decodes version 1 of the encoding system into an array of ints
	 * @param sequence	the byte array to be decoded
	 * @return the int array representing the vector
	 */
	public static int[] decodeV1(byte[] sequence) throws VexNotRecognizedException {
		
		// Reject a non-version 1 encoded sequence
		if (sequence[0] != 0x10 && sequence[0] != 0x1C) {
			throw new VexNotRecognizedException();
		}
		
		// Create an array list to hold the output
		ArrayList<Integer> numbers = new ArrayList<Integer>(sequence.length);
		
		// For each byte, ignoring the header
		for (int i = 1; i < sequence.length; i++) {
			
			byte b = sequence[i];
						
			switch (b) {
				// Two-byte number:
				case (byte)0xFE:
					// Shift into an int, with & masks to deal with issues where JVM marks the numbers negative
					int number = 0xFFFF & ((sequence[i+1] << 8) | (sequence[i+2] & 0xFF));
					numbers.add((int)number);
					
					// Increment by two, because two new bytes were read ahead
					i += 2;
					break;
				// Sequence of zeros:
				case (byte)0xFF:
					// Get the two-byte number of zeros (see above)
					int numberOfZeros = 0xFFFF & ((sequence[i+1] << 8) | (sequence[i+2] & 0xFF));
					
					// Increment by two, because two new bytes were read ahead
					i += 2;
					// Add the correct number of zeros to the output
					for (int j = 0; j < numberOfZeros; j++) {
						numbers.add(0);
					}
					break;	
				default:
					// Frustratingly, this is the easiest way to convert primitive byte to Integer
					numbers.add(new Integer(new Byte(b).intValue()));
			}
		}
		
		int[] out = convertToIntArray(numbers);
		
		return out;
		
	}
	
	/**
	 * Converts an ArrayList<Integer> to int[]
	 * @param intList	an ArrayList of Integers
	 * @return A vector in the form of an array of integers
	 */
	protected static int[] convertToIntArray(ArrayList<Integer> intList) {
		int[] out = new int[intList.size()];
		
		for (int i = 0; i < intList.size(); i++) {
			Integer integer = intList.get(i);
			out[i] = integer.intValue();
		}
		
		return out;
	}
	
	/*
	 * Caps all ints passed at 16-bit unsigned limit
	 * @param number	the number to cap
	 * @return the capped int
	 */	
	public static int shortCeiling(int number) {
		if (number > 65535) {
			return 65535;
		} else if (number < 0) {
			return 0;
		} else {
			return number;
		}
	}
	
	/*
	 * Prints an array of integers to the console
	 * @param array	array to print
	 */
	protected static void printArray(int[] array) {
		for (int i = 0; i < array.length; i++) {
			System.out.print(array[i] + " ");
		}
		
		System.out.println("");
	}
	
	/*
	 * Decode a byte sequence, while checking the version
	 * @param sequence	the byte sequence to decode
	 * @return a vector represented as an array of ints
	 */
	public static int[] decode(byte[] sequence) throws VexNotRecognizedException {
		if (sequence[0] != 0x10 && sequence[0] != 0x1C) {
			return decodeV1(sequence);
		} else {
			throw new VexNotRecognizedException();
		}
	}
	
	// Basic tests, more to come
	public static void main(String[] args) throws VexNotRecognizedException {
		int[] array = {1, 27, 0, 0, 0, 0, 0, 0, 333, 4, 200000};
		System.out.println(DatatypeConverter.printHexBinary(encode(array)));
		printArray(decodeV1(encode(array)));
		
		System.out.println(DatatypeConverter.printHexBinary(encodeCompressed(array)));
		printArray(decodeV1(encodeCompressed(array)));
		
		
		byte[] bytes = {(byte)0x1C, 3, 7, (byte)0xFF, 0, 15};
		printArray(decodeV1(bytes));
	}
}