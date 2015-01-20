cricket-template
================

Template manager originally designed for use in the cricket.

Uses [handlebars](http://handlebarsjs.com/) [java](https://github.com/jknack/handlebars.java) for templating and a custom XML-ish language for minecraft component support.

Language
--------

Raw text stays raw text: `Test` -> `Test` (or `"Test"` in minecraft component JSON).

To insert parameters use handlebars syntax: `ID: {{id}}` could be transformed to `ID: 4`.

### Components

After the templating pass is done (all handlebars entities mentioned above are inserted), the XML is converted to components. While raw text is just converted to text, various component features are available:

```
Normal text

<span color="red">
    Red text

    <lf/>

    <click action="open_url" value="http://cricket.yawk.at">
        Best ticketing plugin
    </click>

    <hover action="show_text" value="Online">
        Yawkat
    </hover>
</span>
```

Spaces, newlines and tabs are collapsed to a single space. `<lf>` is used to start a new line.

The available event types are `<hover>`, `<click>` and `<click shift="true">`.

The available actions are listed [here](https://github.com/yawkat/mcomponent/blob/master/src/main/java/at/yawk/mcomponent/action/BaseAction.java#L59-66), note that, however, some are limited to specific events. The `value` tag supports component XML if the action accepts it.
