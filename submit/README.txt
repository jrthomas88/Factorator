Instructions

Typing 'make all' will compile the program.

Start by launching the FactorServer program.  Launch it as following:

	java model.FactorServer <flag> <arguments>

Possible flag and argument values are:

	-g b: generate two random prime numbers p and q and multiply them together.
			Each of the primes will be a b-bit number, and thus will not exceed 2^b-1.

	-n n: give the program a specified integer n to factor.  p and q will not be known.

	-r b: generate a random b-bit number to factor.  This number is not guaranteed to be a
			factor of merely two primes.

	-pq p q: given integers p and q, factor n=pq

All timing tests were done using the -g flag.

Once the server is running, launch the client.  It could be on the same machine, or a
different one.  Launch the client as following:

	java model.FactorClient <hostname>

Where hostname is the name of the machine on the network that is running the server.  Once the
client is running, return to the server and type 'factor'.  This will send a message to the
clients to begin factoring.  Then wait for the clients to send back the factor.  The clients
shut themselves down automatically.  After the run is completed, the server will create a results.txt file that will contain factorization information and timing results.
