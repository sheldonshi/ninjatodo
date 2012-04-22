/*
	This file is a part of myTinyTodo.
	(C) Copyright 2010 Max Pozdeev <maxpozdeev@gmail.com>
	Licensed under the GNU GPL v3 license. See file COPYRIGHT for details.
*/

// AJAX myTinyTodo Storage

(function(){

var mtt;

function mytinytodoStorageAjax(amtt) 
{
	this.mtt = mtt = amtt;
}

window.mytinytodoStorageAjax = mytinytodoStorageAjax;

mytinytodoStorageAjax.prototype = 
{
	/* required method */
	request:function(action, params, callback)
	{
		if(!this[action]) throw "Unknown storage action: "+action;

		this[action](params, function(json){
			if(json.denied) mtt.errorDenied();
			if(callback) callback.call(mtt, json)
		});
	},


	loadLists: function(params, callback)
	{
		$.getJSON(this.mtt.mttUrl+'ToDoLists/loadLists?rnd='+Math.random(), callback);
	},


	loadTasks: function(params, callback)
	{
		var q = '';
		if(params.search && params.search != '') q += '&s='+encodeURIComponent(params.search);
		if(params.tag && params.tag != '') q += '&t='+encodeURIComponent(params.tag);
		if(params.setCompl && params.setCompl != 0) q += '&changeShowCompleted=1';
		q += '&rnd='+Math.random();

		$.getJSON(this.mtt.mttUrl+'ToDos/loadTasks?list='+params.list+'&showCompleted='+params.showCompleted+'&sort='+params.sort+q, callback);
	},


	newTask: function(params, callback)
	{
		$.post(this.mtt.mttUrl+'ToDos/newTask',
			{ list:params.list, title: params.title, tag:params.tag }, callback, 'json');
	},
	

	fullNewTask: function(params, callback)
	{
		$.post(this.mtt.mttUrl+'ToDos/saveFullTask',
			{ list:params.list, title:params.title, note:params.note, prio:params.prio, tags:params.tags, duedate:params.duedate },
			callback, 'json');
	},


	editTask: function(params, callback)
	{
		$.post(this.mtt.mttUrl+'ToDos/saveFullTask',
			{ id:params.id, title:params.title, note:params.note, prio:params.prio, tags:params.tags, duedate:params.duedate },
			callback, 'json');
	},


	editNote: function(params, callback)
	{
		$.post(this.mtt.mttUrl+'ToDos/editNote', {id:params.id, note: params.note}, callback, 'json');
	},


	completeTask: function(params, callback)
	{
		$.post(this.mtt.mttUrl+'ToDos/completeTask', { id:params.id, completed:params.completed }, callback, 'json');
	},


	deleteTask: function(params, callback)
	{
		$.post(this.mtt.mttUrl+'ToDos/deleteTask', { id:params.id }, callback, 'json');
	},


	setPrio: function(params, callback)
	{
		$.getJSON(this.mtt.mttUrl+'ToDos/setPriority?id='+params.id+'&prio='+params.prio+'&rnd='+Math.random(), callback);
	},

	
	setSort: function(params, callback)
	{
		$.post(this.mtt.mttUrl+'ToDoLists/setListSortOrder', { list:params.list, sort:params.sort }, callback, 'json');
	},

	changeOrder: function(params, callback)
	{
        var order = {};
        for(var i in params.order) {
            var key = params.order[i].diff;
            if (order[key]) {
                order[key].push(params.order[i].id);
            } else {
                order[key] = [params.order[i].id];
            }
		}
        var paramMap = {};
        for (var key in order) {
            if (!paramMap.id && order[key].length == 1) {
                paramMap.id = order[key][0];
            } else if (key > 0) {
                paramMap.back = order[key];
            } else if (key < 0) {
                paramMap.forward=order[key];
            }
        }
		$.post(this.mtt.mttUrl+'ToDos/changeOrder', paramMap, callback, 'json');
	},

	tagCloud: function(params, callback)
	{
		$.getJSON(this.mtt.mttUrl+'ToDos/tagCloud?list='+params.list+'&rnd='+Math.random(), callback);
	},

	moveTask: function(params, callback)
	{
		$.post(this.mtt.mttUrl+'ToDos/moveTask', { id:params.id, from:params.from, to:params.to }, callback, 'json');
	},

	parseTaskStr: function(params, callback)
	{
		$.post(this.mtt.mttUrl+'ajax.php?parseTaskStr', { list:params.list, title:params.title, tag:params.tag }, callback, 'json');
	},
	

	// Lists
	addList: function(params, callback)
	{
		$.post(this.mtt.mttUrl+'ToDoLists/addList', { name:params.name }, callback, 'json');

	},

	renameList:  function(params, callback)
	{
		$.post(this.mtt.mttUrl+'ToDoLists/renameList', { list:params.list, name:params.name }, callback, 'json');
	},

	deleteList: function(params, callback)
	{
		$.post(this.mtt.mttUrl+'ToDoLists/deleteList', { list:params.list }, callback, 'json');
	},

	publishList: function(params, callback)
	{
		$.post(this.mtt.mttUrl+'ajax.php?publishList', { list:params.list, publish:params.publish },  callback, 'json');
	},
	
	setShowNotesInList: function(params, callback)
	{
	    $.post(this.mtt.mttUrl+'ajax.php?setShowNotesInList', { list:params.list, shownotes:params.shownotes },  callback, 'json');
	},
	
	setHideList: function(params, callback)
	{
		$.post(this.mtt.mttUrl+'ajax.php?setHideList', { list:params.list, hide:params.hide }, callback, 'json');
	},

	changeListOrder: function(params, callback)
	{
		$.post(this.mtt.mttUrl+'ToDoLists/changeListOrder', { order:params.order }, callback, 'json');
	},

	clearCompletedInList: function(params, callback)
	{
		$.post(this.mtt.mttUrl+'ToDoLists/clearCompletedInList', { list:params.list }, callback, 'json');
	}

};

})();