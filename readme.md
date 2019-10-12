# The Craftlang programming language #

_Craftlang_ is a programming language designed to easily create Minecraft data packs.

Compared to other Minecraft data pack programming languages, Craftlang focuses on compiling code that works _at
runtime_. This means that Craftlang is much more than just a preprocessor unrolling some loops. This programming
language supports writing any procedural code not involving pointers; recursion is also supported.

## How to use ##

To create Minecraft data packs using Craftlang, you need the Craftlang compiler, _craftlangc_, which you can obtain
[here](https://github.com/craftlang/craftlangc/releases/latest). You also need [Java](https://www.java.com/download/) 8
or greater. The compiler is a runnable Java archive, which means it is executable using `java -jar craftlangc.jar`.

To create a data pack, create one or more _Craftlang source files_. `.craftlang` is the recommended extension for such
files, however no particular extension is enforced by the compiler.

Each source file has to contain one or more function definitions. Once you run the compiler, all those functions will be
properly compiled into `.mcfunction` files, and all those `.mcfunction` files will be placed in the correct folder
structure in order to produce a working data pack.

## A basic example ##

In the following example you can see how Craftlang can be used to perform some basic operations. A more detailed
explanation of all the language features and some of the inner workings of the compilation process is provided later.

```craftlang
namespace some.namespace

# Sums a to b and returns the result
fun sum(a: int, b: int): int
	sum = a + b

# Calculates the factorial
fun factorial(x: int): int
	if x >= 0
		if x == 0
			factorial = 1
		else
			factorial = factorial(x - 1) * x

# Run this once the data pack has loaded
tag load
fun onload()
	/schedule function this:is/your/namespace/scheduled 2s

fun scheduled()
	var i = 0
	while i <= 10
		factorial(i)
		/execute as @e[tag=cr_frame] if score @s cr_id = #cr cr_fp run tellraw @a ["factorial(",{"score":{"name":"@s","objective":"cr_local_1"}},") = ",{"score":{"name":"#cr","objective":"cr_ret_1"}}]
		i += 1
```

Compiling the code with `java -jar craftlangc.jar <source> -o <destination>` will generate a folder at `destination`,
which is a data pack which you can grab and plug into your world for testing. If you wish, a zip file can be generated
instead of a plain folder by using `java -jar craftlangc.jar <source> -oz <destination>`.

## Base 62 ##

The Craftlang compiler creates many new scoreboard objectives, often called `cr_<something>_<number>`. Because of the
16-character objective name limitation of Minecraft, `number` is expressed in base 62 in order to save space.

Base 62 is a numbering system with 62 digits. With this numbering system, 0<sub>62</sub> = 0, 1<sub>62</sub> = 1, ...,
8<sub>62</sub> = 8, 9<sub>62</sub> = 9, A<sub>62</sub> = 10, B<sub>62</sub> = 11, ..., Y<sub>62</sub> = 34,
Z<sub>62</sub> = 35, a<sub>62</sub> = 36, b<sub>62</sub> = 37, ..., y<sub>62</sub> = 60, z<sub>62</sub> = 61.

## Identifiers, names and fully qualified names ##

Minecraft allows to organize the functions of a data pack by some rather arcane rules. It is for example possible to
create a math data pack with functions callable with `/function math:sqrt`, `/function math:trig/sin`,
`/function math:trig/cos`, ...

Minecraft likes to differentiate between the namespace of a function (in the previous example, `math`) and the path of a
function (`sqrt`, `trig/sin`, `trig/cos` respectively). The name of the function (`sqrt`, `sin`, `cos`) is embedded in
its path. To fully qualify a function, both the namespace and the path are needed: `math:trig/sin`.

Craftlang has its own mechanism to fully qualify names, which is both simpler and more similar to how it is done in
other programming languages. In particular, a _fully qualified name_ (_FQN_) is in the form `<namespace>.<name>`. For
instance, the `sin` function would be fully qualified as `math.trig.sin`. Notice how for Craftlang the namespace is
anything preceding the name.

### Namespaces and functions ##

Each Craftlang source file can optionally declare its namespace (if not, it defaults to `minecraft`). All functions
defined in the same source file are bound to that namespace. The FQN of a function is given by `<namespace>.<name>`,
where `name` is the name of the function.

```craftlang
# Here the namespace is declared
namespace this.is.a.namespace

# FQN: this.is.a.namespace.sum
fun sum(a: int, b: int): int
	sum = a + b

# FQN: this.is.a.namespace.some_function
fun some_function()
	# First way to call sum
	sum(1, 2)

	# Second way to call sum
	this.is.a.namespace.sum(1, 2)
```

Functions defined within the same namespace can be called using simply their name. However, to call functions defined
elsewhere their FQN is needed.

A function with FQN `this.is.a.namespace.some_function` will be callable within Minecraft using
`/function this:is/a/namespace/some_function`.

### Global variables ###

Example:

```craftlang
namespace this.is.your.namespace

# FQN: this.is.your.namespace.a
var a: int

fun hello()
	a = 12
	this.is.your.namespace.a *= 2
```

The semantics of using global variables is the same as that of functions. However, the identifier of the objective
generated for a given global variable has _no_ relationship with the name of the variable!

Minecraft has no way of namespacing scoreboard objectives names; also, names are limited to 16 characters at most. This
is the rationale behind the decision of not using the variable name as the objective name.

## Defining and using functions ##

Functions are defined by the word `fun`, followed by a name, a list of 0 or more parameters and the body of the
function. If the function returns a value, its type has to be specified:

```craftlang
# A function taking two integers as arguments and returning an integer as result
fun sum(a: int, b: int): int
	# The variable sum is implicitly declared to store the result of the function
	sum = a + b

# A function that takes no arguments and returns no value
fun say_hello()
	/tellraw @a "Hello!"

# A function that takes no arguments and explicitly returns no value (return type = void)
fun say_bye(): void
	/tellraw @a "Bye!"
```

If a function is defined to return a value, a variable with the same name as the function will be implicitly declared.
This variable has to be used to store the value the function returns after execution.

### Tagging ###

Functions can optionally be tagged. This way, functions can be called in bulk using `/function #<tag>`.

To tag a function, insert one or more `tag <FQN>` lines before the function definition. If the FQN is just in the form
`<name>` (with no namespace), `minecraft` will be assumed to be the namespace.

```craftlang
# Single tag function
tag world.weather.nicify
fun set_day()
	/time set 6000

# Multi tag function
tag world.weather.nicify
tag world.weather.clear
fun clear_weather()
	/weather clear

# Function tagged minecraft.load (default namespace is applied)
tag load
fun set_day_and_clear_weather()
	/function #world:weather/nicify
```

### Calling convention ###

Sometimes, you'll want to call functions from raw Minecraft code, without relying on Craftlang. In that case, you'll
have to follow the Craftlang calling convention:

 * For each argument of the function, from left to right, set the objective `cr_arg_<n>` of the player `#cr` to the
   appropriate value. `n` is the argument number starting at 1, expressed in base 62.
 
 * If the function returns a value, it will be stored in the objective `cr_ret_1` of the player `#cr`.

For example, the equivalent of calling `math.trig.atan2(2, 1)` would be:

```mcfunction
scoreboard players set #cr cr_arg_1 2
scoreboard players set #cr cr_arg_2 1
function math:trig/atan2
# The return value is now stored in the score cr_ret_1 of the player #cr 
```

## Variables ##

Craftlang supports both _global_ and _local_ variables. A variable is global if it is declared outside of all function
definitions.

Each variable has its own scope and can't be accessed outside of it. Global variables are accessible anywhere; local
variables are only accessible within the same block of code they're defined in.

```craftlang
namespace some.namespace

# Declare a global integer variable called a. Global variables can only be declared. You can assign values to them in
# functions
var a: int

fun some_function(arg_1: int, arg_2: int)
	# arg_1 and arg_2 are two integer variables accessible by the whole function

	# Set the global variable a to 14
	a = 14

	# Declare and assign (that is, define) a new variable, b. Its type is automatically determined by the type of the
	# assigned value (in this case, it would be an integer).
	var b = a + 1

	if b > a
		# Declare a new boolean variable c. The type is mandatory since assignment is deferred until later. As in the C
		# programming language, the value of a variable is undefined until assignment
		var c: bool
		
		# Assign to c the value of false
		c = false

	# After the if statement, the c variable declared previously goes out of scope and thus isn't accessible any more
	
	# When defining a new variable, the type can be stated explicitly. If the assigned value doesn't match the expected
	# type, an error is raised
	var d: int = a * 3
```

For now, Craftlang supports only integer (`int`) and boolean (`bool`) types. Support for fixed point numbers and records
is planned for the future.

Like the C programming language, Craftlang supports the following shorthand operators: `*=`, `/=`, `%=`, `+=`, `-=`,
`&=`, `^=`, `|=`. In contrast to C, assignments _cannot_ be used as expressions. This means that `var b *= 2 + (a = 3)`
is _not_ a valid statement!  

## Expressions ##

Craftlang supports the set of expressions defined below. Binary expressions with the same precedence level are always
left-associative. The semantic of each expression is equivalent to that of the C programming language.

<table>
	<thead>
		<tr>
			<th>Precedence</th>
			<th>Expression</th>
			<th>Syntax</th>
			<th>Requires</th>
			<th>Produces</th>
		</tr>
	</thead>
	<tr>
		<td>7</td>
		<td>Unary</td>
		<td><code>+&lt;operand&gt;</code></td>
		<td><code>int</code></td>
		<td><code>int</code></td>
	</tr>
	<tr>
		<td>7</td>
		<td>Unary</td>
		<td><code>-&lt;operand&gt;</code></td>
		<td><code>int</code></td>
		<td><code>int</code></td>
	</tr>
	<tr>
		<td>7</td>
		<td>Unary</td>
		<td><code>!&lt;operand&gt;</code></td>
		<td><code>bool</code></td>
		<td><code>bool</code></td>
	</tr>
	<tr>
		<td>6</td>
		<td>Multiplicative</td>
		<td><code>&lt;left&gt; * &lt;right&gt;</code></td>
		<td><code>int</code>, <code>int</code></td>
		<td><code>int</code></td>
	</tr>
	<tr>
		<td>6</td>
		<td>Multiplicative</td>
		<td><code>&lt;left&gt; / &lt;right&gt;</code></td>
		<td><code>int</code>, <code>int</code></td>
		<td><code>int</code></td>
	</tr>
	<tr>
		<td>6</td>
		<td>Multiplicative</td>
		<td><code>&lt;left&gt; % &lt;right&gt;</code></td>
		<td><code>int</code>, <code>int</code></td>
		<td><code>int</code></td>
	</tr>
	<tr>
		<td>5</td>
		<td>Additive</td>
		<td><code>&lt;left&gt; + &lt;right&gt;</code></td>
		<td><code>int</code>, <code>int</code></td>
		<td><code>int</code></td>
	</tr>
	<tr>
		<td>5</td>
		<td>Additive</td>
		<td><code>&lt;left&gt; - &lt;right&gt;</code></td>
		<td><code>int</code>, <code>int</code></td>
		<td><code>int</code></td>
	</tr>
	<tr>
		<td>4</td>
		<td>And</td>
		<td><code>&lt;left&gt; &amp; &lt;right&gt;</code></td>
		<td><code>bool</code>, <code>bool</code></td>
		<td><code>bool</code></td>
	</tr>
	<tr>
		<td>3</td>
		<td>Xor</td>
		<td><code>&lt;left&gt; ^ &lt;right&gt;</code></td>
		<td><code>bool</code>, <code>bool</code></td>
		<td><code>bool</code></td>
	</tr>
	<tr>
		<td>2</td>
		<td>Or</td>
		<td><code>&lt;left&gt; | &lt;right&gt;</code></td>
		<td><code>bool</code>, <code>bool</code></td>
		<td><code>bool</code></td>
	</tr>
	<tr>
		<td>1</td>
		<td>Comparison</td>
		<td><code>&lt;left&gt; == &lt;right&gt;</code></td>
		<td><code>int</code>, <code>int</code></td>
		<td><code>bool</code></td>
	</tr>
	<tr>
		<td>1</td>
		<td>Comparison</td>
		<td><code>&lt;left&gt; == &lt;right&gt;</code></td>
		<td><code>bool</code>, <code>bool</code></td>
		<td><code>bool</code></td>
	</tr>
	<tr>
		<td>1</td>
		<td>Comparison</td>
		<td><code>&lt;left&gt; != &lt;right&gt;</code></td>
		<td><code>int</code>, <code>int</code></td>
		<td><code>bool</code></td>
	</tr>
	<tr>
		<td>1</td>
		<td>Comparison</td>
		<td><code>&lt;left&gt; != &lt;right&gt;</code></td>
		<td><code>bool</code>, <code>bool</code></td>
		<td><code>bool</code></td>
	</tr>
	<tr>
		<td>1</td>
		<td>Comparison</td>
		<td><code>&lt;left&gt; &lt;= &lt;right&gt;</code></td>
		<td><code>int</code>, <code>int</code></td>
		<td><code>bool</code></td>
	</tr>
	<tr>
		<td>1</td>
		<td>Comparison</td>
		<td><code>&lt;left&gt; &lt; &lt;right&gt;</code></td>
		<td><code>int</code>, <code>int</code></td>
		<td><code>bool</code></td>
	</tr>
	<tr>
		<td>1</td>
		<td>Comparison</td>
		<td><code>&lt;left&gt; &gt;= &lt;right&gt;</code></td>
		<td><code>int</code>, <code>int</code></td>
		<td><code>bool</code></td>
	</tr>
	<tr>
		<td>1</td>
		<td>Comparison</td>
		<td><code>&lt;left&gt; &gt; &lt;right&gt;</code></td>
		<td><code>int</code>, <code>int</code></td>
		<td><code>bool</code></td>
	</tr>
</table>

Function calls, like `sum(3, 4)` and `math.trig.sin(45)` have a precedence value of 8 and are computed before unary
expressions are evaluated. Parentheses can be used to force a specific order of operations; in particular, parenthesized
expressions have a precedence value of 9 and are evaluated even before function calls.

As an example, the expression `2 + 3 * 4 * 5 <= sum(1, 2) / 3 - 1 != (factorial(4) - 2) / 3` would be equivalent to
`((2 + ((3 * 4) * 5)) <= ((sum(1, 2) / 3) - 1)) != ((factorial(4) - 2) / 3)`.

## Control flow ##

Similarly most programming languages, Craftlang supports _if_, _if-else_, _while_ and _do-while_ statements; however,
the language does not provide C-like for loops.

Like all statements, the use of control flow statements is only permitted within a function definition. Such statements
can only operate on boolean expressions. This is an important distinction to languages like C or JavaScript, which can
derive the "truthyness" of any expression, even if it is not a boolean expression:

```craftlang
fun is_zero(x: int)
	# The following if statement is not valid, as it is not testing a boolean expression
	if x
		/tellraw @a "x is not 0"
	else
		/tellraw @a "x is 0"

	# This is OK
	if x != 0
		/tellraw @a "x is not 0"
	else
		/tellraw @a "x is 0"
```

Control flow statements can be nested:

```craftlang
var meh = 1
var x = 1
while x <= 10
	var y = 0
	while y <= 10
		if x >= y
			meh *= x
		y += 1
	x += 1
```

### If statements ###

If statements allow for conditional branching. An if statement tests for its condition, and if it is true it executes a
block of code. Optionally, it can also execute another block of code if the condition evaluates to false.

```craftlang
# Simple if statement
if x != 0
	/tellraw @a "x is not equal to 1"

# If-else statement
if x > 0
	/tellraw @a "x is greater than 0"
else
	/tellraw @a "x is not greater than 0"

# If-else-if statement
if x > 0
	/tellraw @a "x is greater than 0"
else if x < 0
	/tellraw @a "x less than 0"
else
	/tellraw @a "x is 0"
```

### While statements ###

While statements allow for a block of code to be repeatedly executed as long as a given condition holds true. First the
condition is checked, and then (if the condition is true) the block of code is executed.

```craftlang
var i = 0
while i < 10
	/tellraw @a "This will be printed 10 times"
	i += 1
```

### Do-while statements ###

Do-while are similar to while statements, in that they allow for a block of code to be repeatedly executed as long as a
condition holds true. In contrast to while statements, the condition is checked _after_ the block of code has executed;
this means that the block of code of a do-while statement will always execute at least once.

```craftlang
var i = 10
do
	/tellraw @a "This will be printed once even though it is not true that i < 10"
	i += 1
while i < 10
```

## Raw Minecraft code ##

Craftlang allows the insertion of raw Minecraft code in the generated `.mcfunction` files. The syntax for doing so is:

```craftlang
/<code>
```

The content of `code` is simply inserted in the respective `.mcfunction` file. No checking is done to ensure that the
inserted code is a valid Minecraft command. This allows for some neat debugging tricks, as for example `/# Some comment`
will write an actual comment to the function file.

Using raw Minecraft code is of central importance. Most of the examples above used raw Minecraft code in some way.

Interaction between raw Minecraft code and Craftlang's generated code is not too smooth. Many times, you'll have to look
at the generated code and then deduce how to interact with it. If you have some suggestions on how to improve the
exchange of data between raw Minecraft code and generated code, please let me know. For now, you can rely on the
following certainties which are a consequence of the Craftlang calling convention:

 * At the beginning of a function, the arguments of the function will be stored in the player `#cr` using the objectives
   `cr_arg_1`, `cr_arg_2`, ... There are no guarantees as to what happens to the values of those scores as the function
   executes, however using raw Minecraft code immediately as the first statements should avoid the problem altogether:
   
   ```craftlang
   function print(x: int)
   	/tellraw @a [{"score":{"name":"#cr","objective":"cr_arg_1"}}]
   
   function increment(x: int): int
   	/scoreboard players add #cr cr_arg_1 1
   	/scoreboard players operation #cr cr_ret_1 = #cr cr_arg_1
   ```

 * Immediately after a call to a non void function returns, the objective `cr_ret_1` of the player `#cr` will be set to
   the result provided by the called function. Again, there is no guarantee as to what happens to that score later on;
   once more, this problem is solved by using the score immediately:
   
   ```craftlang
   function random(): int
   	# So random!
   	random = 42
   
   function print_random()
   	# No assignment to a variable is needed, the score is set whether it is used or not
   	random()
   	/tellraw @a ["The randomly generated value is: ",{"score":{"name":"#cr","objective":"cr_ret_1"}}]
   ```