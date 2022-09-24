# SifriTaub • Assignment 3

## Authors
* David Deres
* Adan Abo Hyeah

## Notes

### Implementation Summary

As a continuation for the last assignment we started off by modifying and refactoring our existing 
code to support asynchronous programming (we ended up doing that for almost all the classes we implemented).

after that, in-order to implement the loaning system as required in the assigment 
we added two fields to SifriTaub, loansDB which is the database used to store the actual loans, 
and loansQueue which is the actual queue for the loan requests submitted by users.
and then moved forward to implement the actual functions which used the databases from the previous
assigment in addition to these two fields.


### Testing Summary

before testing the new loaning system, we first refactored our tests of the previous assignment to support asynchronous
programming by integrating CompleteableFuture to them, we made sure that they all pass.
after that we thought about edge cases regarding the loaning system and wrote tests checking that.

first we had to implement a mock of the additional external library provided in the assignment.

after that, the way we wrote the tests was to go over each function , test it, then understand how it can be used with the rest of the functions
and then writing a test involving all the methods previously tested. actually by doing so we made sure we "unit test"-ed
our implementation of SifriTaub and the loaning system specifically,
and only after that we worked on implementing more serious tests involving the whole interface.


### Difficulties

- learning about CompleteableFuture and understanding the correct way to use them and modify our interface to work with them.
we had to look up alot of things, even though everything is explained in a wonderful way in some websites, still we needed some guidance
with what to search for from Dor (and it helped us alot, thank you!).
- fixing the issues with guice and the persistent storages we faced in the previous assignment.
- understanding the way loaning system's functions worked together, the documentation of them was sometimes a bit confusing.

