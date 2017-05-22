/*
 * Copyright (c) 2007 Josh Bush (digitalbush.com)
 * 
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:

 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE. 
 */
 
/*
 * Version: .1
 * Release: 2007-08-10
 */ 
(function($) {
	//Global Methods/Properties
	$.grid={
		//Plugin Attachment Location
		plugins:{}
	};	
	
	//Main jQuery function
	$.fn.grid=function(settings) {		
		settings = $.extend({}, settings);
		return this.each(function(){
			var table=$(this);			
			$.each($.grid.plugins,function(n){			
				this.call(table,settings[n]);
			});
		});
	};

	//Plugins
	$.extend($.grid.plugins,{
		
		navigate:function(settings){
			/*
			* Still Needs:						
			* - scroll to element when visibility goes away.
			* - multi-select option via ctrl+click, shift+click and shift/ctrl + arrows
			*/			
			if(!settings)
				return;
				
			settings = $.extend(
			{
				selectedClass: 'selected',
				activatedClass: 'activated',
				startUpSelect:false,
				maintainSelection:true,
				multiselect:false //Planned addition for future
			}, settings);			
			
			var table=$(this);
			var tbody=$("tbody",this);
			if(!tbody.length)
				tbody=table;
			
			var selector="tr";  //Here in case we decide to expand to cell and or column selection		
						
			function select(obj){				
				if(obj && !$(obj).is("."+settings.selectedClass)){
					$(selector+"."+settings.selectedClass,tbody).removeClass(settings.selectedClass);
					$(selector+"."+settings.activatedClass,tbody).removeClass(settings.activatedClass);
					$(obj).addClass(settings.selectedClass).focus();					
					table.trigger("RowSelected",[$(obj)]);					
				}		
			}

			function activate(obj){
				if(obj){
					$(selector+"."+settings.activatedClass,tbody).removeClass(settings.activatedClass);
					$(obj).addClass(settings.activatedClass);
					table.trigger("RowActivated",[$(obj)]);
				}		
			}
			
			if(settings.startUpSelect)
				select($(selector+":first",tbody)[0]);
			
			//Thanks to Dan G. Switzer
			function findTarget(el){
			    var $el = $(el);
		        return ($el.is(selector)) ? $el : $el.parents(selector);
		    }			
			
			tbody.click(function(e){select(findTarget(e.target));});
			
			tbody.dblclick(function(e){activate(findTarget(e.target));});
			
			table.focus(function(e){				
				if(!$(selector+"."+settings.selectedClass,tbody).length)
					select($(selector,tbody)[0]);
			});
			
			table.blur(function(e){
				if(!settings.maintainSelection){
					$(selector+"."+settings.selectedClass,tbody).removeClass(settings.selectedClass);
					$(selector+"."+settings.activatedClass,tbody).removeClass(settings.activatedClass);
				}
			});			
			
			table.keydown(function(e){
				var k = e.keyCode;
				switch (k){
					case 40: // arrow down						
						var $obj=$(selector+"."+settings.selectedClass,tbody).next();
						select($obj[0]);				
						e.preventDefault();
						break;
					case 38: // arrow up						
						var $obj=$(selector+"."+settings.selectedClass,tbody).prev();
						select($obj[0]);				
						e.preventDefault();
						break;											
					case 13:
						activate($(selector + "." + settings.selectedClass)[0]);
						e.preventDefault();
						break;			
				}
			});
		},		
		scroll:function(settings){
			/*
			* Still Needs:						
			* - a way to not need explicit column declarations
			* - dimensions plugin usage to better handle borders and padding
			* - a way to handle multi-row thead and tfoot
			* - a way to handle colspans in thead,tfoot
			*/
			if(!settings || !settings.width)
				return;
				
			var realWidth=settings.width-19;
			
			$.each(settings.colWidths,function(i){
				realWidth=realWidth-this;
			});
			settings.colWidths[settings.colWidths.length-1]=settings.colWidths[settings.colWidths.length-1]+realWidth;
			
			var table=$(this);
			var thead=$("thead",table);
			var tfoot=$("tfoot",table);
			var tbody=$("tbody",table);
			
			var topPadding=0;
			var bottomPadding=0;
			if(thead.length)
				topPadding=thead.height();
			if(tfoot.length)
				bottomPadding=tfoot.height();
			
			
					
			var wrapper=[]
			wrapper.push("<div class='scrollableTableWrapper' style='position:relative;width:"+settings.width+"px;padding-top:"+topPadding+"px;padding-bottom:"+bottomPadding+"px; clear:left'>");
			wrapper.push("<div class='scrollableTableInnerWrapper' style='overflow:auto;width:auto;height:auto'>");
			wrapper.push("</div></div>");
			table.wrap(wrapper.join(''));
			/*
			$("tr",thead).css({position:"absolute",top:0,left:0});
			$("tr",tfoot).css({position:"absolute",bottom:0,left:0});
			$("tr:eq(0) th,tr:eq(0) td",thead).each(function(i){
				$(this).width(settings.colWidths[i]+'px');
			});
			$("tr:eq(0) th,tr:eq(0) td",tfoot).each(function(i){
				$(this).width(settings.colWidths[i]+'px');
			});
			
			
			var bodyRow=false;
			
			$("tr:eq(0) td",tbody).each(function(i){
				bodyRow=true;
				$(this).width(settings.colWidths[i]+'px');
			});
			
			if(!bodyRow){
				table.one("CollectionChanged",function(e,action,rows){
					if(action=="add"){
						$("th,td",$(rows)[0]).each(function(i){
							$(this).width(settings.colWidths[i]+'px');						
						});
					}
				});
			}*/
		},
		stripe:function(settings){
			if(!settings)
				return;
				
			settings = $.extend({
				evenClass: 'even',
				oddClass: 'odd'
			}, settings);
			
			var table=$(this);
			var tbody=$("tbody",this);
			if(!tbody.length)
				tbody=table;
			function stripe(){
				$("tr:visible",tbody).each(function(i){
					$(this).removeClass(settings.evenClass).removeClass(settings.oddClass).addClass(i%2==0?settings.evenClass:settings.oddClass);
				});
			}			
			table.bind("CollectionChanged",stripe);
			stripe();
		},
		columnResize:function(settings){
			if(!settings)
				return;
				
			settings = $.extend(
			{
				minWidth:75,
				handleCss:{cursor:"col-resize",width:1,borderRight:"1px dotted black"}
			}, settings);
			var table=$(this);
			var thead = $("thead",table);
			var tbody = $("tbody",table);
			var tfoot = $("tfoot",table);
			
			var headers=[];			
			var resizing;
			
			$("tr:first th",thead).not(":last").each(function(i){										
				var th=$(this);
				var res=$("<div class='tableColumnResizer' style='float:right'>&nbsp;</div>").css(settings.handleCss);
				$(res).mousedown(function (e) {
					var nextHeader=$("tr:first th:eq("+(i+1)+")",thead);
					resizing = {
						index:i,
						startX:e.clientX,
						startWidth:th.width(),
						nextWidth:nextHeader.width(),												
						header:th,
						nextHeader:nextHeader,												
						col:$("tr td:eq("+i+")",tbody),
						nextCol:$("tr td:eq("+(i+1)+")",tbody),
						footer:$("tr th:eq("+i+")",tfoot),
						nextFooter:$("tr th:eq("+(i+1)+")",tfoot)
					};	
						return false;							
				});					
				$(this).prepend(res);
			});
			
			$(document).mousemove(function(e){
				if (resizing) {
					var diff = e.clientX-resizing.startX;					
					var newWidth = resizing.startWidth + diff;					
					var nextWidth = resizing.nextWidth-diff;
					if (newWidth > settings.minWidth && newWidth< (resizing.startWidth+resizing.nextWidth-settings.minWidth)) { 
						resizing.header.width(newWidth+"px");
						resizing.nextHeader.width(nextWidth+"px");

						resizing.col.width(newWidth+"px");												
						resizing.nextCol.width(nextWidth+"px");					

						resizing.footer.width(newWidth+"px");
						resizing.nextFooter.width(nextWidth+"px");
						
					}
				}
			});		
			
			$(document).mouseup(function(e){
				if (resizing) 
					resizing = false;				
			});
				
		}
	});

	//A few jQuery methods add-ons to aid in row manipulation
	$.extend($.fn,{		
		addRow:function(data,index){
			this.trigger("AddRow",[data,index]);
		},
		removeRow:function(index){
			this.trigger("RemoveRow",[index]);
		}
	});
})(jQuery);