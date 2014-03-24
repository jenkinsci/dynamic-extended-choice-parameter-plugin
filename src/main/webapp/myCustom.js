function get_parent(bindfieldname)
{
			var allElements = document.getElementsByName('name');
			var element="";
			var bindfieldname=bindfieldname;
			for (var i = 0; i < allElements.length; i++)
			{
				if (allElements[i].value == bindfieldname)
				{
					element = allElements[i];
				}
			}
			return element;
}