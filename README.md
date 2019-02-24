# 4HBButtons
android app: 4 bluetooth holding buttons

Sends data to the connected device like this:

Let's say the slider is at 120 and I will name it %speed_value% in this example

# Left top button:
focus_value: `<375>`
focus_code: `"<" + (255 + %speed_value%) + ">"`

defocus_value: `<255>`
defocus_code: `<255>`

# Left bottom button:
focus_value: `<135>`
focus_code: `"<" + (255 - %speed_value% ) + ">"`
//TODO add more info
