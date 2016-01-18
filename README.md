# Vex, a vector encoder

Vex encodes integer vectors into a custom format for uses such as storage in databases.  Vex currently only supports numbers up to 16-bits in size.

Considerations for future versions of Vex include increasing supported number size and compressing sequences of other numbers than zero.  With a header byte denoting each version, anything encoded with this version of Vex will still be able to be read by future versions.