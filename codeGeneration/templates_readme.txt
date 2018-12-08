Using templates for code generation:

TEMPLATE COMMENTS
'//<' Indicates a template comment, these comments will be skipped entirely
and will no longer be visible in the generated code.

example:
//< This is a template comment line

TEMPLATE VARIABLES
It is possible to define variables to use in the template with:
$var$ variable_identifier variablue_value
example, replacing every occurrence of 'foo' with 888 :
$var$ foo 888

SUB-TEMPLATES
It is also possible to include sub-templates with :
$template$ template_name
The code generator will replace such lines with that template's contents.
Subtemplates can contain further subtemplates and so on.

NOTE ON VARIABLES IN SUBTEMPLATES :
Variable values defined in a subtemplate will only apply within the scope
of that subtemplate.
Variables from parent templates carry over! there is no warning for this!
Variables from parent templates can be redefined with a different value,
the generator will offer warnings when a parent's variable is redefined.


UPDATING settings.json
To actually start using a new base template, it has to specified as the
template to use for one or multiple files within settings.json.

settings.json specifies the files to generate in the following format:
	{
		"filename" : "output_filename",
		"base_template" : "base_template_name",
		"parameters" : "params_to_use_for_file"
	}

output_filename : mandatory, specifies the filename to use for the generated
	output.
base_template : optional, specifies the base template from which the new file
	should be generated. If it is not specified the generator will look for a
	base template with the same name as output_filename.
parameters : mandatory, specifies the parameters to use when generating the
	output file. Certain lines within