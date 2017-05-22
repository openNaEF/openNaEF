import voss.nms.inventory.script.renderer.groovy.DateFormat
import voss.nms.inventory.script.renderer.groovy.GroovyUtils
import voss.nms.inventory.script.renderer.groovy.TemplateImporter

print "■■■■■■■■■■■■■■■■running base.groovy■■■■■■■■■■■■■■■■\n"


dir = params.dir
currentTime = DateFormat.run("yyyy/MM/dd HH:mm", new Date())
title = ""

//  <j:import uri="script_title_builder_simple.jelly" inherit="true"/>
//def title = ""
//if(title.length() > 200) {
//  title=title.substring(0,200)
//}
pw.println('<?xml version="1.0" encoding="Windows-31J"?>')
pw.println("<script operation_type='新設' title='" + title + "' start_date='" + currentTime + "'>")
pw.println('  <explain>')
//pw.println('    '+param.explain)
pw.println('  </explain>')

basicConfigContexts.each() { context ->
    print "calll TemplateImporter \n"
    TemplateImporter.run(dir, "", context.scriptUri,
            GroovyUtils.getEntry("params", params), GroovyUtils.getEntry("context", context), GroovyUtils.getEntry("pw", pw))
}
pw.println('</script>')

print "■■■■■■■■■■■■■■■■quit base.groovy■■■■■■■■■■■■■■■■\n"