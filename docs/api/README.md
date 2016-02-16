# Server API
Chiori-chan's Web Server utilizes a proprietary API based on the PHP API for ease of use. The primary scripting language is Groovy and supports Groovy Server Pages (GSP), which much like PHP allows a developer can place groovy coode within code blocks starting  with `<%` and ending with `%>`.

    You are running <% getProduct() %> <% getVersion() %>
Output `You are running Chiori-chan's Web Server 9.3.6 (Milky Polkadot)`

While Groovy is the default and preferred scripting language, Chiori-chan's Web Server supports the addition of scripting languages by way of plugins. Implementing this API will depend of how each language is implemented, i.e., scripts need to extend the `com.chiorichan.factory.api.Builtin` class. See [LuaPlugin](plugins/LuaPlugin.md) for an example.

Please note, that while it was attempted to replicate the PHP API, the PHP syntax was not. You will have to follow the Groovy syntax, e.g., foreach and array is PHP specific.

[Groovy Language Syntax](http://groovy-lang.org/syntax.html)

```php
PHP: foreach( $vars as $var ) {}
Groovy: vars.each { var -> }

PHP: $var = array("val1", "val2", "val3")
Groovy: def var = ["val1", "val2", "val3"]

PHP: $var = array("key1" => val1", "key2" => "val2", "key3" => "val3")
Groovy: def var = [key1: val1", key2: "val2", key3: "val3"]
```

# API References
## Variable and Type Related Methods
### `int count( map,collection,array,string )`
Counts all elements in a map, collection, array, or string

```php
print count( obj )
```

Output
* `[key1: "val1", key2: "val2"]` -> `2`
* `["obj1", "obj2", "obj3"]` -> `3`
* `"This is a test string!"` -> `22`
    
### `boolean is_array( object )`
Determines whether an object is an instance of array or collection

```php
def var = ["obj1", "obj2", "obj3"]
print is_array( var )
```

Output `True`

```php
def var = "This is a test string!"
print is_array( var )
```

Output `False`

### `boolean is_float( object )`
Determines whether an object is an instance of Float

* `print is_float( "abc" )` -> `False`
* `print is_float( 23 )` -> `False`
* `print is_float( 23.5 )` -> `True`
* `print is_float( True )` -> `False`

### `boolean is_int( object )`
Determines whether an object is an instance of Integer

* `print is_int( 23 )` -> `True`
* `print is_int( "23" )` -> `False`
* `print is_int( 23.5 )` -> `False`
* `print is_int( True )` -> `False`
* `print is_int( False )` -> `False`

### `String base64Encode( bytes,String )`
Encodes the specified argument into a String using the Base64 encoding scheme.

* `print base64Encode( "This is a test string!" )` -> `VGhpcyBpcyBhIHRlc3Qgc3RyaW5nIQ==`
* `print base64Encode( "Any type of byte array".getBytes() )` -> `Ynl0ZSBhcnJheQ==`

