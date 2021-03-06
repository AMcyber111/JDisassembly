---
layout: default
title: Machine code Analysis
image:
  path: https://repository-images.githubusercontent.com/24021024/5e678080-0cfe-11eb-9edf-294da025f0c1
---

<h1>Machine code Introduction.</h1>

In the <a href="https://recoskie.github.io/JDisassembly/docs/Basics.html">basics document</a> we discussed the basic primitive data types that processors can work with and do operations with.

<br />

In this document, we will discuss basic machine code and what we mean by system architecture. Thus what instruction sets are.

<br />

When comparing an x86 core from the '80s like the 16 bit Intel 8086 to a modern AMD ryzen, the first thought that will most likely run through your heads is that they run entirely different machine code, but it is not true.

<br />

Modern x86 can run 16 bit 8086 machine code in 16-bit mode, and the instructions only switch to 16 bit but do not change encoding. This is called the processor <strong>instruction set architecture</strong>.

<br />

Even though it is an AMD core, it still runs the same x86 machine code as an Intel x86.

<br />

A processor that is <strong>architecture x86</strong> means it runs x86 machine code natively without translation. This means binary software wrote in the '80s still runs on any modern x86 core made by any company.

<br />

A processor has a foundational instruction set that can do arithmetic with the basic primitive data types and at least do comparison.

<br />

The binary encoded instructions the processor understands as a particular command do not change in later x86 cores.

<br />

However, the internal design of the circuits may change, but the binary encoding for each instruction does not change.

<br />

<h1>Instruction encoding.</h1>

A single 0 to 255-byte value is used, for which operation you wish the CPU to do in an x86 core. This is called the opcode.

<br />

The bytes that come after the instruction number are what the instruction uses as input.

<br />

This <a href="http://www.mlsite.net/8086/" target="_blank">link</a> is a mapping of the original 8086 instructions. Without any added instructions that became available in later cores.

<br />

The 8086 map is the original foundation for x86 machine code. Thus modern x86 cores, even AMD ones, still run the same basic operations using the same encoding.

<br />

For now, let's start with a few sample codes.

<br />

A processor has variables called registers which operations are completed with. In this example, we use the ADD operation code 02.

<br />

~~~nasm
MOV AH,97
MOV BX,3333
ADD AH, BYTE PTR[BX]
~~~

<br />

Machine code:<br />

~~~
B4 97
BB 33 33
02 27
~~~

<br />

MOV is short for move. MOV sets the value of the AH register 97, and then BX is set 3333. The ADD operation uses the BX register as a location to a byte that is added with AH.

<br />

There are two 8 bit ADD operations. The other ADD operation ADD's and stores the result at a memory location. This is ADD operation code 00.

<br />

~~~nasm
MOV AH,97
MOV BX,3333
ADD BYTE PTR[BX], AH
~~~

<br />

Machine code:<br />

~~~
B4 97
BB 33 33
00 27
~~~

<br />

There are also two 16 bit ADD operations that do the same operation. Codes 01 and 03.

<br />

Lastly, there also are two ADD operations that add a register by the next two bytes or one byte after the ADD operation.

<br />

Add operation opcode 04.

<br />

~~~nasm
MOV AL, 97
ADD AL, 33
~~~

<br />

Machine code:<br />

~~~
B4 97
04 33
~~~

<br />

And two-byte (16 bit add) add operation code 05.

<br />

~~~nasm
MOV AX, 9763
ADD AX, 3333
~~~

<br />

Machine code:<br />

~~~
B8 97 63
05 33 33
~~~

<br />

This makes a total of 6 ADD operations. This is why there are multiple ADD operation codes.

<br />

The two MOV operation codes we have been using are B0 to B7 hex, which puts the next byte into a selected register, and operation codes B8 to BF hex, which moves the next two bytes into the selected register.

<br />

There are 8 general arithmetic registers in total, so there is 8 MOV operation, for 8 bit, and 16-bit move.

<br />

So you can key in this entire binary code yourself on an AMD or Intel x86 core if you liked.

<br />

<strong>Or you can use an assembler program. Which an assembler program uses an x86 instruction map to generate the operation codes for you.</strong>

<h2>32 bit x86.</h2>

With the introduction of 32 bit, the 16-bit operations are made 32 bit's long. Thus two-byte add also became 4 byte add. The 8-bit operations remain 8 bit's in length.

<br />

Operation code 66 hex is used to make 32-bit operations switch to the original 16-bit version. After the instruction completes, it switches back to 32 bits.

<br />

So operation code 66 hex become known as the <strong>operand override prefix</strong>. As it was used before every 32-bit operation to make the operation the original 16-bit operation.

<br />

This way, both 16-bit operations and 32-bit operations can be mixed in 32-bit mode.

<br />  

<strong>A bit mode setting was added that lets you set the CPU 16-bit mode.</strong>

<br />

In 16 bit mode, all 32-bit operations are their original 16-bit size without using the operand override prefix 66 hex before every operation.

<br />

This allowed 16 bit 8086 to be directly run as it would on a 16-bit core. Plus with the prefix code 66 in 32 bit allowed both 16 bit and 32 bit to be mixed.

<h2>64 bit x86.</h2>

With the introduction of 64 bit by AMD, all instructions stayed 32 bit. So using 66 before a 32-bit operation allowed the operation to go 16 bit.

<br />  

A new code was added that could only be used in 64-bit mode. The REX prefix uses operation codes 40 to 4F.

<br />

The REX Prefix allowed us to set 64 bits before the next operation and 3 additional settings.

<br />

<strong>When we set bit mode 32 bit. The instructions 40 to 4F are usable again, as the REX prefix is disabled.</strong>

<br />

Meaning 32-bit machine code has full backward compatibility without any software emulation or translation.

<br />

Thus Intel uses AMD's 64-bit REX prefix as it is a good system. It does not affect compatibility to the original 16 bit 8086 instruction to 32 bit.

<br />

So even today, the most modern x86 cores still run the same operation codes with prefixes that change the size of the next operation code.

<h2>Two byte instruction codes.</h2>

Both Intel and AMD run out of room to put operation codes in 0 to 255 as each instruction code must have a unique number to ensure that older software runs without translation.

<br />  

So the instruction code 0F hex is removed from later processor cores that originally did the operation "POP CS".

<br />

The instruction "POP CS" was never really used. So getting rid of the instruction affect no software programs that used the operation.

<br />

Instead, code 0F read the next byte as a new operation code. Giving a new set of operations 0 to 255. And a new two-byte instruction code map.

<br />

This was also done with two-byte instructions 0F 38 and 0F 3A. So codes 38 and 3A extend to two more sets of 0 to 255 instruction codes under the tow byte map, which are called three-byte opcode maps.

<h2>Adding operation codes to x86.</h2>

Intel and AMD used prefix operations to add additional settings that expanded x86 without breaking compatibility with older machine code.

<br />

And to not break compatibility with operating systems that are compiled in x86 machine code. Such as DOS, Windows, UNIX, MAC OS X x86, and even Linux x86. 

<br />
This is how machine code is supposed to work. <strong>As an x86 is an instruction set architecture.</strong> Every operation code must use a unique operation code, for each operation.

<br />

Say you are working at AMD and wish to implement a new operation code under 0F 77. However, you find out Intel already uses that two-byte operation code for the <strong>EMMS</strong> operation.

<br />

You can include the patented instruction <strong>EMMS</strong>, from Intel into your AMD x86 core. However, you are not allowed to put your own AMD operation code here.

<br />

You have to find ways of adding in your operations that do not use other operation codes taken by other companies. So companies keep x86 instruction architecture maps to make it easy to see opcodes not yet used.

<br />

This ensures that every operation code has a unique operation code number and can not be anything other than its operation.

<br />

There have been actual legal battles in which AMD and Intel fight for operation code space.

<br />

Also, x86 is old, and companies have to be careful with how they add new operation codes to the x86 instruction map. Without causing problems with x86 compiled operating systems and software.

<h1>Operation size naming.</h1>

Because 16-bit, 32-bit, and 64-bit code can be mixed in x86 binary code using prefixes. With 8-bit operations having their own operation code.

<br />  

The registers are given different names based on the size of an operation.

<br />

We have 8 general purposes registers that we can use with arithmetic operation codes. Each register has a three-bit code that is used to select which register to use with the operation code.

<br />

<table border="1">
  <tr><td>code</td><td>000</td><td>001</td><td>010</td><td>011</td><td>100</td><td>101</td><td>110</td><td>111</td></tr>
  <tr><td>r64</td><td>RAX</td><td>RCX</td><td>RDX</td><td>RBX</td><td>RSP</td><td>RBP</td><td>RSI</td><td>RDI</td></tr>
  <tr><td>r32</td><td>EAX</td><td>ECX</td><td>EDX</td><td>EBX</td><td>ESP</td><td>EBP</td><td>ESI</td><td>EDI</td></tr>
  <tr><td>r16</td><td>AX</td><td>CX</td><td>DX</td><td>BX</td><td>SP</td><td>BP</td><td>SI</td><td>DI</td></tr>
  <tr><td>r8</td><td>AL</td><td>CL</td><td>DL</td><td>BL</td><td>AH</td><td>CH</td><td>DH</td><td>BH</td></tr>
</table>

<br />

When the size of the operation changes, so does the register size.

<br />

The "A" register is called RAX when it is 64 in length.<br />
The "A" register is called EAX when it is 32 in length.<br />
The "A" register is called AX when it is 16 in length.

<br />

When doing a 64 in length, add with "RAX", and say we go to read the value of "AX". We then see the first 16 digits of the add operation out of 64.

<br />

The registers are given a different name based on the size. Meanwhile, the selected register is still the same.

<br />

There is only one small catch. All 8-bit operations use the first 4 registers in low and switch to high byte order.

<br />

The "A" register is called AL when it is the first 8 bit's of the "A" register.<br />
The "A" register is called AH when it is the next 8 bits of the "A" register.

<br />

Say we set the AX value 8877 hex. The value of AH is then 88, and the value of AL is then 77 hex. Only 8-bit operations use this.

<br />

When reading memory, we use BYTE (8 bit), WORD (16 bit), DWORD (32 bit), QWORD (64 bit). For the size of the number, we wish to read and ADD with the size of our register.

<br />

~~~nasm
ADD RAX, QWORD PTR [RDI]
ADD EAX, DWORD PTR [RDI]
ADD AX, WORD PTR [RDI]
~~~

<br />

Machine code:<br />

~~~
48 03 07
03 07
66 03 07
~~~

<br />

This is still the 16 bit ADD operation 03 in 8086.

<br />

However, in 64-bit mode, it is 32 in length by default. The register that is used as the memory location becomes 64 in length.<br />
In 32 bit code or 32-bit mode, the register RDI in the address would be EDI as 32 in length.

<br />

48 hex is used before the operation code 03 to make it 64 bit in size. Lastly, 66 hex is used before operation code 03 to make it 16 bit.

<br />

Using codes 48 or 66 before an 8-bit operation does nothing to the 8 in size operation.

<h1>Operand encoding.</h1>

~~~
48 03 07 = ADD RAX, QWORD PTR [RDI]
~~~

<br />

The value 07 after the operation code is 00000111 binary. The binary splits apart as follows 00, 000, 111. The first two digits are the mode. The 3 digits after the mode are the register code. The last 3 digits are the register code for the address.

<br />

This is called a ModR/M byte. This is used with every register and memory operation in x86. When the first two-mode bits are set 11, then the address is switched to a register.

<br />

So 11, 101, 011 = EB.

<br />

48 03 EB = ADD RBP,RBX

<br />

Thus register code 101 is RBP, and code 011 is registered RBX. Do not forget that the registers change names based on the length of the register being used.

<br />

The ModR/M encoding is what makes x86 operations flexible. Switch mode to 00 with 00, 101, 011 = 2B.

<br />

~~~
48 03 2B = ADD RBP,QWORD PTR [RBX]
~~~

<br />

The selected register moves into the memory address as a location. The other two-mode setting 01 and 10, add a byte after the ModR/M to the address.

<br />

So 01, 101, 011 = 6B.

<br />

~~~
48 03 6B 72 = ADD RBP,QWORD PTR [RBX+72]
~~~

<br />

The next byte is added to the address. This is called an 8-bit displacement.

<br />

And finally 10, 101, 011 = AB.

<br />

~~~
48 03 AB 11 22 33 44 = ADD RBP,QWORD PTR [RBX+44332211]
~~~

<br />

The next 4 bytes are added to the address. This is called a 32-bit displacement, also called a dword displacement.

<br />

<strong>As flexible as the ModR/M byte seamed. More was added to it. The register code 100 is not usable in the address.</strong>

<br />

So 00, 101, 100 = 2C.

<br />

~~~
48 03 2C 00 = ADD RBP,QWORD PTR [ RAX + RAX ]
~~~

<br />

When register code 100 is used. The next value becomes two register selections added together for the address.

<br />

So 00, 101, 100 = 2C. ModR/M byte.
Then 00, 001, 100 = 0C. SIB byte.

<br />

~~~
48 03 2C 0C = ADD RBP,QWORD PTR [RSP+RCX]
~~~

<br />

The second byte is called the SIB address. We can choose any two registers we wish to add together in the address. This is called the index, plus base address. The first 2 digits are 00 = *1, 01 = *2, 10 = *4, 11 = *8.

<br />

So 00, 101, 100 = 2C. ModR/M byte.
Then 10, 001, 100 = 8C. SIB byte.

<br />

~~~
48 03 2C 8C = ADD RBP,QWORD PTR [RSP+RCX*4]
~~~

<br />

Lastly, the displacement mode in the ModR/M is added after the SIB byte.

<br />

So 00, 101, 100 = 2C. ModR/M byte.
Then 10, 001, 100 = 8C. SIB byte.

<br />

~~~
48 03 AC 8C 11 22 33 44 = ADD RBP,QWORD PTR [ RSP + RCX * 4 + 44332211 ]
~~~

<br />

This made the address system very flexible.

<br />

This makes the total instruction encoding for all operations as follows.

<br />

<img src="Figs/x86-fig1.gif" />

<br />

This is the instruction format that is used with all binary instructions. Even to 8086. From <a href="https://www.intel.com/content/dam/www/public/us/en/documents/manuals/64-ia-32-architectures-software-developer-vol-2a-manual.pdf#page=35" target="_blank">64 ia 32 architectures software developer</a>.

<br />

You can also use the Intel x86 architecture reference, for AMD x86 cores. As x86 is the instruction set architecture. See <a href="https://www.felixcloutier.com/x86/" target="_blank">AMD Instruction list.</a> Derived by Intel architecture manual.

<br />

Even though it is an AMD core, it still runs the same x86 machine code as an Intel x86. As x86 is the <strong>instruction set architecture</strong>. So, in reality, the official documentation by Intel is better.

<br />

There is not much left to teach you here about x86 encoded instructions. You can, however test out your coding skills with <a href="https://recoskie.github.io/X86-64-Disassembler-JS/Basic%20Live%20View.html" target="_blank">Web x86 code disassembler</a>.

<br />

The three other settings I did not go in-depth about in the REX prefix are called register extension bits. It added an extra binary digit to your selected register codes in the ModR/M and SIB. This upped the number of usable registers from 8 to 16.

<br />

<strong>Also, some institutions only use one input. Such as left shift a value or right shift a value.</strong>

<br />

In these instructions, the first register code was never used in ModR/M. So instead of wasting instruction space. The unused register was used to select from 8 instructions that used one input.

<br />

Additionally, the register names can change depending on the operation. For example, vector operations use MMX registers flowed by the selected register number in your ModR/M byte. The Instruction encoding, however, does not change the format.

<br />

Even the FPU uses the same instruction format. The only thing changing is the register you are picking from and the word size of memory.

<br />

My advice to you is to test the encodings yourself and to learn from the Intel document.

<br />

You may also like <a href="http://www.mlsite.net/blog/?p=55">Things that were not immediately obvious to me</a>. Preview bellow.

<br />

<img src="Figs/x86-fig2.gif" />

<h1>ARM architecture.</h1>

Now, let's switch to different processor architecture. Companies that create ARM cores also have to keep instruction maps as well as the company has to find unused instructions not used by other companies to add new instructions in ARM.

<br />

Even though it is a snapdragon ARM core, it still runs the same ARM machine code as an Apple bionic ARM core. As ARM is the <strong>instruction set architecture</strong>. So, in reality, the official documentation by ARM is best.

<br />

People had fun with ARM in iPhone. Creating emulators that recompiled code into ARM code. It's not hard to do as the machine code is not a big secret.

<br />

The internal circuits can change. However, the instruction encodings do not. ARM as a company has all the listings for all ARM instruction encodings, which are freely available.

<br />

<img src="Figs/fig5.gif" />

<br />

Apple does not like people building emulators. So they made it that the binary is checked before it loads, for if it jumps a core to manually generated codes, which is called JIT compilation. They also eliminate any workaround that allows you to get the CPU to run bytes you write to RAM as code.

<br />

Nothing is stopping anyone from looking at any part of the IOS system. You can decode any part to what it does in IOS. Suppose you wish to spend the time to disassemble ARM core codes from the ARM code map or use an ARM disassembler.

<br />

Taking the IOS system apart makes it easy to build a jailbreak; however, it is a long and slow process.

<br />

<strong>Microsoft even created Windows RT, which is a complete recompilation of windows. Allowing Windows to run on ARM cores. However, all x86 Microsoft programs could not be loaded on Windows ARM.</strong>

<br />

It required every developer to recompile their code into ARM machine code as windows does not translate binary code in EXE files. It is expected that the CPU runs the binary code without error by the Architecture type.

<br />

<strong>Unlike x86, where there is a binary operation for everything.</strong> ARM focuses on doing things in software with a smaller instruction set. This is why we call an <stong>x86 core a CISC core and an ARM a RISC core</stong>.

<br />

<strong>CISC means Complex Instruction Set Computing. Thus RISC means Reduced instruction set computing.</strong>

<br />

ARM cores may not have a bunch of operations codes, however, can be clocked much higher and generate less heat. Use less power. So in a way, fewer operation codes are better. Since all instructions can be created using a few simple arithmetic operation codes.

<br />

All ARM codes are 32 bits long in the memory, unlike x86, which has variable-length instructions.

<br />

The following link is a simple introduction to ARM technology. Used in cell phones, and other mobile devices: <a href="http://www.csbio.unc.edu/mcmillan/Comp411F18/Lecture06.pdf">Link</a>.

<br />

In a way, ARM has a much more flexible instruction set. Thus instructions can be conditional to the result of the previous instruction. Making it easy to build complex functions using a reduced instruction set.

<br />

The following <a href="https://developer.arm.com/architectures/cpu-architecture/a-profile/docs">link</a> is from ARM.

<br />

Plus, all you have to do is map the instructions. Write an Assembler if you like. You can also write your own programming language if you liked. The same can be said for x86.

<br />

It is just a matter of what instruction architecture you want your system to run on. Thus what type of cores you want to be stuck to that manufactures will make.

<br />

Thus the same is true for operating systems. As you must pick an architecture language that the whole system will run in.

<h1>Self modifying code.</h1>

It turns out we can write self-modifying code once you understand the basics of processor architecture. You can then write byte sequences out in memory and run them directly.

<br />

There are still developers that know how to write code that compares data and makes changes on its own. It is not magic once you know what processor architecture is.

<br />

Self-modifying code is very powerful in creating realistic artificial intelligence systems that perform quickly without any slow compiler, or interpreter, or Assembler level tool between.

<h1>Malicious code.</h1>

One of the problems with processor design is that it must be mapped and standardized for software to work or run.

<br />

It, however, makes it easy to write binary code that infects all systems since machine code is architecture-based and not Vendor or company-specific.

<br />

This makes it easy for people to do a thing called code injection. Which is something I worry about. Thus I think you should be too. However, there is not much we can do.

<br />

We came up with an instruction set encryption. It is not practical, though, as it requires the encryption key set in the CPU core to change after so much time.

<br />

Each time the encryption key changes, it then has to decrypt and re-encrypt the entire program. So if another code tries to write to a running binary, it then would have to know the current encryption key set in the CPU core.

<br />

IT works well when security is a must. However, it is slow and is not practical. Also, we can not use encryption keys that always remain the same for a particular process or data as it is easy to break encryption keys.

<br />

Also, the following document, <a href="https://scholarworks.sjsu.edu/cgi/viewcontent.cgi?referer=https%3A%2F%2Fwww.google.com%2F&httpsredir=1&article=1154&context=etd_projects">Approximate Disassembly</a>, from the University of San Jose State discusses how to detect such code.

<br />

No matter how good of a detection model we make, it is not full proof to those that design the code to not be detected.

<br />

This is why detection software must be updated constantly, as it is not hard to make stuff pass through the scanner undetected.