NAME = Subway.class

SRCS = $(wildcard *.java)
OBJS = $(SRCS:.java=.class)

all : $(OBJS)

%.class : %.java
	javac $<

clean : 
	@rm -rf $(OBJS)
	@printf "make clean\n"

re : 
	@make clean
	@make all

.PHONY : all clean re
