/*
 This file is based on Max Pozdeev's myTinyTodo (C) Copyright 2009-2010 Max Pozdeev <maxpozdeev@gmail.com>
 (C) Copyright 2012 Sheldon Shi
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
			if(json && json.denied) mtt.errorDenied();
			if(callback) callback.call(mtt, json)
		});
	},

    invitations: function(params, callback) {
        $.getJSON(this.mtt.mttUrl+'Invitations/index?projectId='+params.project+'&rnd='+Math.random(), callback);
    },

    invite: function(params, callback) {
        $.post(this.mtt.mttUrl+'Invitations/invite',
            { projectId:params.project, emails:params.emails, role:params.role }, callback, 'json');
    },

	loadLists: function(params, callback)
	{
		$.getJSON(this.mtt.mttUrl+'ToDoLists/loadLists?projectId='+params.project+'&rnd='+Math.random(), callback);
	},


	loadTasks: function(params, callback)
	{
		var q = '';
		if(params.search && params.search != '') q += '&s='+encodeURIComponent(params.search);
		if(params.tag && params.tag != '') q += '&t='+encodeURIComponent(params.tag);
		if(params.setCompl && params.setCompl != 0) q += '&changeShowCompleted=1';
		q += '&rnd='+Math.random();

		$.getJSON(this.mtt.mttUrl+'ToDos/loadTasks?projectId='+params.project+'&list='+params.list+'&showCompleted='+params.showCompleted+'&sort='+params.sort+q, callback);
	},


	newTask: function(params, callback)
	{
		$.post(this.mtt.mttUrl+'ToDos/newTask',
			{ list:params.list, title: params.title, tag:params.tag }, callback, 'json');
	},
	

	saveFullTask: function(params, callback)
	{
		$.post(this.mtt.mttUrl+'ToDos/saveFullTask', params, callback, 'json');
	},

    loadTask: function(params, callback)
    {
        $.getJSON(this.mtt.mttUrl+'ToDos/index?taskId='+params.id, callback);
    },

    cloneTask: function(params, callback)
    {
        $.post(this.mtt.mttUrl+'ToDos/cloneTask',
            { taskId:params.id}, callback, 'json');
    },

	completeTask: function(params, callback)
	{
		$.post(this.mtt.mttUrl+'ToDos/completeTask', { taskId:params.id, completed:params.completed }, callback, 'json');
	},

    checkNote: function(params, callback)
    {
        $.post(this.mtt.mttUrl+'ToDos/checkNote', { taskId:params.taskId, noteId:params.noteId, checked:params.checked }, callback, 'json');
    },

	deleteTask: function(params, callback)
	{
		$.post(this.mtt.mttUrl+'ToDos/deleteTask', { taskId:params.id }, callback, 'json');
	},


	setPrio: function(params, callback)
	{
		$.getJSON(this.mtt.mttUrl+'ToDos/setPriority?taskId='+params.id+'&prio='+params.prio+'&rnd='+Math.random(), callback);
	},

	
	setSort: function(params, callback)
	{
		$.post(this.mtt.mttUrl+'ToDoLists/setListSort', { list:params.list, sort:params.sort, projectId:params.project }, callback, 'json');
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
            if (!paramMap.taskId && order[key].length == 1) {
                paramMap.taskId = order[key][0];
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
		$.getJSON(this.mtt.mttUrl+'ToDoLists/tagCloud?list='+params.list+'&projectId='+params.project+'&rnd='+Math.random(), callback);
	},

    addTag: function(params, callback) {
        $.post(this.mtt.mttUrl+'ToDos/addTag', {taskId:params.id,tags:params.tags}, callback, 'json');
    },

    checkNotification: function(params, callback)
    {
        $.getJSON(this.mtt.mttUrl+'Notifications/check?rnd='+Math.random(), callback);
    },

    newNotificationCount: function(params, callback) {
        $.getJSON(this.mtt.mttUrl+'Notifications/checkNewCount?rnd='+Math.random(), callback);
    },

    clearNotification: function(params, callback) {
        $.post(this.mtt.mttUrl+'Notifications/clear', params, callback, 'json');
    },

	moveTask: function(params, callback)
	{
		$.post(this.mtt.mttUrl+'ToDos/moveTask', { taskId:params.id, from:params.from, to:params.to }, callback, 'json');
	},

	parseTaskStr: function(params, callback)
	{
		$.post(this.mtt.mttUrl+'ajax.php?parseTaskStr', { list:params.list, title:params.title, tag:params.tag }, callback, 'json');
	},

	// Lists
	addList: function(params, callback)
	{
		$.post(this.mtt.mttUrl+'ToDoLists/addList', { name:params.name,projectId:params.project }, callback, 'json');

	},

    addProject: function(params, callback)
    {
        $.post(this.mtt.mttUrl+'Projects/add', { title:params.title }, callback, 'json');

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
	    $.post(this.mtt.mttUrl+'ToDoLists/toggleNotesExpanded', { list:params.list, notesExpanded:params.notesExpanded },  callback, 'json');
	},

    setWatchList: function(params, callback) {
        $.post(this.mtt.mttUrl+'ToDoLists/toggleWatchedByMe', { list:params.list, watchedByMe:params.watchedByMe },  callback, 'json');
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
	},

    deleteInvitation: function(params, callback) {
        $.post(this.mtt.mttUrl+'Invitations/delete', {id:params.id, projectId:params.project}, callback, 'json');
    },

    promoteToAdmin: function(params, callback) {
        $.post(this.mtt.mttUrl+'Projects/promoteToAdmin', {participationIds:params.participations, projectId:params.project}, callback)
    },

    deleteMember: function(params, callback) {
        $.post(this.mtt.mttUrl+'Projects/deleteMember', {participationId:params.id, projectId:params.project}, callback)
    }

};

})();