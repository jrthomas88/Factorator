# Factorator
A distributed project running different factorization algorithms to factor a number n that is a product of primes.  This program utilizes 4 different methods of integer factorization:

1. Trial division from 2.  Simply iterate through potential factors starting at 2.
2. Trial division from sqrt(n).  Start at the sqrt(n) and iterate down through potential factors.
3. Fermat's factorization method.  Find two integers a and b such that n = a^2 - b^2.
4. Pollard's p-1 algorithm.  Find a bound B that that B! contains all factors of p-1.  This can be used to find a non-trivial factor of n.

in the submit directory, use the command 'make move' to copy the source files over.  Type 'make all' to compile the program from there.

To run the program, launch the FactorServer program by using the command 'java model.FactorServer -g [bit-length], where bit-length is the bit-length of two randomly generated primes that will be factored.  You can also use the arguments -n [n], where n is a composite number of your choosing.

Once the server is running, go to any machine running on the same network and run 'java model.FactorClient [hostname], where hostname is the name of the machine running FactorServer.  You can connect an arbitrary number of clients.

Once all clients have connected, go to the machine running the server and type 'factor'.  The server will send the command to all of the clients and print out the factors once found.  Keep in mind that factoring is slow, and for large enough primes this will likely never terminate.
