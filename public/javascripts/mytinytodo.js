/*
	This file is based on Max Pozdeev's myTinyTodo (C) Copyright 2009-2010 Max Pozdeev <maxpozdeev@gmail.com>
    (C) Copyright 2012 Sheldon Shi
	Licensed under the GNU GPL v3 license. See file COPYRIGHT for details.
*/

var ninjatodo = (function(){

var taskList = new Array(), taskOrder = new Array();
var filter = { completed:0, search:'', due:'' };
var sortOrder; //save task order before dragging
var searchTimer;
var objPrio = {};
var selTask = 0;
var flag = { needAuth:false, isLogged:false, tagsChanged:true, readOnly:false, editFormChanged:false };
var taskCnt = { total:0, past: 0, today:0, soon:0 };
var tabLists = {
	_lists: {},
	_length: 0,
	_order: [],
	_alltasks: {},
	clear: function(){
		this._lists = {}; this._length = 0; this._order = [];
		this._alltasks = { id:-1, showCompleted:0, sort:'DEFAULT' };
	},
	length: function(){ return this._length+1; },
	exists: function(id){ if(this._lists[id] || id==-1) return true; else return false; },
	add: function(list){ this._lists[list.id] = list; this._length++; this._order.push(list.id); },
	replace: function(list){ this._lists[list.id] = list; },
	get: function(id){ if(id==-1) return this._alltasks; else return this._lists[id]; },
	getAll: function(){ var r = []; for(var i in this._order) { r.push(this._lists[this._order[i]]); }; return r; },
	reorder: function(order){ this._order = order; }
};
var curList = 0;
var tagsList = [];

var mytinytodo = window.mytinytodo = _mtt = {

	theme: {
		newTaskFlashColor: '#ffffaa',
		editTaskFlashColor: '#bbffaa',
		msgFlashColor: '#ffffff'
	},

	actions: {},
	menus: {},
	mttUrl: '',
	templateUrl: '',
	options: {
		openList: 0,
		singletab: false,
		autotag: false,
		tagPreview: true,
		tagPreviewDelay: 700, //milliseconds
		saveShowNotes: false,
		firstdayofweek: 1,
		touchDevice: false
	},
    project: '',
    editJSLoaded: false,
    tagSuggestUrl: '',

	timers: {
		previewtag: 0
	},

	lang: {
		__lang: null,

		daysMin: [],
		daysLong: [],
		monthsShort: [],
		monthsLong: [],

		get: function(v) {
			if(this.__lang[v]) return this.__lang[v];
			else return v;
		},
		
		init: function(lang)
		{
            lang.daysMin = lang['days_min'] ? lang['days_min'].split(",") : null;
            lang.daysLong = lang['days_long'] ? lang['days_long'].split(",") : null;
            lang.monthsMin = lang['months_short'] ? lang['months_short'].split(",") : null;
            lang.monthsLong = lang['months_long'] ? lang['months_long'].split(",") : null;
			this.__lang = lang;
			this.daysMin = this.__lang.daysMin;
			this.daysLong = this.__lang.daysLong;
			this.monthsShort = this.__lang.monthsMin;
			this.monthsLong = this.__lang.monthsLong;
		}
	},

	pages: { 
		current: { page:'tasks', pageClass:'' },
		prev: []
	},

	// procs
	init: function(options)
	{
		jQuery.extend(this.options, options);

		flag.needAuth = options.needAuth ? true : false;
		flag.isLogged = options.isLogged ? true : false;

		if(this.options.showdate) $('#page_tasks').addClass('show-inline-date');
		if(this.options.singletab) $('#lists .mtt-tabs').addClass('mtt-tabs-only-one');

		this.parseAnchor();

		// handlers
		$('.mtt-tabs-add-button').click(function(){
            singleInputDialog('addList', 'addList');
		});

		$('.mtt-tabs-select-button').click(function(event){
			if(event.metaKey || event.ctrlKey) {
				// toggle singetab interface
				_mtt.applySingletab(!_mtt.options.singletab);
				return false;
			}
			if(!_mtt.menus.selectlist) _mtt.menus.selectlist = new mttMenu('slmenucontainer', {onclick:slmenuSelect});
			_mtt.menus.selectlist.show(this);
		});


		$('#newtask_form').submit(function(){
			submitNewTask(this);
			return false;
		});
		
		$('#newtask_submit').click(function(){
			$('#newtask_form').submit();
		});

		$('#newtask_adv').click(function(){
            clearEditForm();
			showEditForm(1);
			return false;
		});
		
		$('#task').keydown(function(event){
			if(event.keyCode == 27) {
				$(this).val('');
			}
		}).focusin(function(){
			$('#task_placeholder').removeClass('placeholding');
			$('#toolbar').addClass('mtt-intask');
		}).focusout(function(){
			if('' == $(this).val()) $('#task_placeholder').addClass('placeholding');
			$('#toolbar').removeClass('mtt-intask');
		});


		$('#search_form').submit(function(){
			searchTasks(1);
			return false;
		});

		$('#search_close').click(function(){
			liveSearchToggle(0);
			return false;
		});

		$('#search').keyup(function(event){
			if(event.keyCode == 27) return;
			if($(this).val() == '') $('#search_close').hide();	//actual value is only on keyup
			else $('#search_close').show();
			clearTimeout(searchTimer);
			searchTimer = setTimeout(function(){searchTasks()}, 400);
		})
		.keydown(function(event){
			if(event.keyCode == 27) { // cancel on Esc (NB: no esc event on keypress in Chrome and on keyup in Opera)
				if($(this).val() != '') {
					$(this).val('');
					$('#search_close').hide();
					searchTasks();
				}
				else {
					liveSearchToggle(0);
				}
				return false; //need to return false in firefox (for AJAX?)
			}		
		}).focusin(function(){
			$('#toolbar').addClass('mtt-insearch');
			$(this).focus();
		}).focusout(function(){
			$('#toolbar').removeClass('mtt-insearch');
		});


        $('.form-row input.note').live('keyup', function(event){
            if(event.keyCode == 27) return;
            var blankInputs = [];
            $.each($('.form-row input.note'), function(i, item) {
                if ($(item).val() == '') {
                    blankInputs.push(item);
                }
            });
            if (blankInputs.length > 1) {
                $(blankInputs[0]).remove();
            } else if (blankInputs.length == 0) {
                $(this).parent().append('<input type="text" name="note" class="in500 note"/>');
            }

        });

		$('#taskview').click(function(){
			if(!_mtt.menus.taskview) _mtt.menus.taskview = new mttMenu('taskviewcontainer');
			_mtt.menus.taskview.show(this);
		});

        $('#projects a').live('click', function(){
            if ($(this)[0].id=='addProject') {
                addProjectDialog();
                return false;
            } else {
                return true;
            }
        });

		$('#mtt_filters .tag-filter .mtt-filter-close').live('click', function(){
			cancelTagFilter($(this).attr('tagid'));
		});

		$('#tagcloudbtn').click(function(){
			if(!_mtt.menus.tagcloud) _mtt.menus.tagcloud = new mttMenu('tagcloud', {
				beforeShow: function(){
					if(flag.tagsChanged) {
						$('#tagcloudcontent').html('');
						$('#tagcloudload').show();
						loadTags(curList.id, function(){$('#tagcloudload').hide();});
					}
				}, adjustWidth:true
			});
			_mtt.menus.tagcloud.show(this);
		});

		$('#tagcloudcancel').click(function(){
			if(_mtt.menus.tagcloud) _mtt.menus.tagcloud.close();
		});

		$('#tagcloudcontent .tag').live('click', function(){
			addFilterTag($(this).attr('tag'), $(this).attr('tagid'));
			if(_mtt.menus.tagcloud) _mtt.menus.tagcloud.close();
			return false;
		});

        $('#help').click(function(){
            if(!_mtt.menus.help) _mtt.menus.help = new mttMenu('helpdetail', {
                adjustWidth:true
            });
            _mtt.menus.help.show(this);
        });

        $('#helpcancel').click(function(){
            if(_mtt.menus.help) _mtt.menus.help.close();
        });

        $('#notification').click(function(){
            if(!_mtt.menus.notification) _mtt.menus.notification = new mttMenu('notificationdetail', {
                adjustWidth:true
            });
            _mtt.db.request('checkNotification', {}, function(json){
                if (json.total < 1) {
                    s = '<h3>'+_mtt.lang.get('notification_no')+'</h3>';
                } else {
                    var s = '<h3>'+_mtt.lang.get('a_notification')+'</h3><ul>';
                    $.each(json.list, function(i,item) {
                        var msgJson = $.parseJSON(item.message);
                        s += '<li>'+_mtt.lang.get(msgJson.message).replace("[s1]",msgJson.s1).replace("[s2]",msgJson.s2).replace("[s3]",msgJson.s3)+'&nbsp;&nbsp;<span>' + smartDate(item.dateCreated, json.now) + '</span></li>';
                    })
                    s += '</ul><div class="notification-action"><a href="#" class="weak">'+_mtt.lang.get('notification_clear')+'</a></div></div>';
                }
                $('#notificationcontent').html(s);
            });
            _mtt.menus.notification.show(this);
            $('#notification strong').remove();
        });

        $('#notificationcontent .notification-action a').live('click', function() {
            _mtt.db.request('clearNotification', {}, function(json) {
                if (json.total == 0) {
                    $('#notificationcontent').html('<h3>'+_mtt.lang.get('notification_no')+'</h3>');
                }
            });
        });

        $('#notificationcancel').click(function(){
            if(_mtt.menus.notification) _mtt.menus.notification.close();
        });

		$('#taskviewcontainer li').click(function(){
			if(this.id == 'view_tasks') setTaskview(0);
			else if(this.id == 'view_past') setTaskview('past');
			else if(this.id == 'view_today') setTaskview('today');
			else if(this.id == 'view_soon') setTaskview('soon');
		});
		
		// Tabs
		$('#lists li.mtt-tab').live('click', function(event){
			if(event.metaKey || event.ctrlKey) {
				// hide the tab
				hideTab(this);
				return false;
			}
			tabSelect(this);
			return false;
		});
		
		$('#list_all').click(function(event){
			if(event.metaKey || event.ctrlKey) {
				// hide the tab
				hideTab(-1);
				return false;
			}
			tabSelect(-1);
			return false;
		});

		$('#lists li.mtt-tab .list-action').live('click', function(){
			listMenu(this);
			return false;	//stop bubble to tab click
		});
		
		$('#list_all .list-action').click(function(event){
			listMenu(this);
			return false;	//stop bubble to tab click
		});

		//Priority popup
		$('#priopopup .prio-neg-1').click(function(){
			prioClick(-1,this);
		});

		$('#priopopup .prio-zero').click(function(){
			prioClick(0,this);
		});

		$('#priopopup .prio-pos-1').click(function(){
			prioClick(1,this);
		});

		$('#priopopup .prio-pos-2').click(function(){
			prioClick(2,this);
		});

		$('#priopopup').mouseleave(function(){
			$(this).hide()}
		);

		$('#taskedit_form').submit(function(){
			return saveTask(this);
		});
		
		$("#duedate").datepicker({
			dateFormat: _mtt.duedatepickerformat(),
			firstDay: _mtt.options.firstdayofweek,
			showOn: 'button',
			buttonImage: _mtt.templateUrl + 'images/calendar.png', buttonImageOnly: true,
			constrainInput: false,
			duration:'',
			dayNamesMin:_mtt.lang.daysMin, dayNames:_mtt.lang.daysLong, monthNames:_mtt.lang.monthsLong
		});

        $('.note-done-action').live('click', function() {
            var el = findParentNode(this, 'DIV');
            if (el) {
                var noteId = el.id.split('_')[1];
                var taskId = parseInt(getLiTaskId(this));

                _mtt.db.request('checkNote', {taskId:taskId, checked:true, noteId:noteId}, function(json){
                    $.each(taskList[taskId].notes, function(i, item) {
                        if (item.id==noteId) {
                            taskList[taskId].notes[i].checked=true;
                            $('#note_'+noteId).removeClass('note-undone').addClass('note-done');
                            return;
                        }
                    });
                });
            }
            return false;
        })

        $('.note-undone-action').live('click', function() {
            var el = findParentNode(this, 'DIV');
            if (el) {
                var noteId = el.id.split('_')[1];
                var taskId = parseInt(getLiTaskId(this));
                _mtt.db.request('checkNote', {taskId:taskId, checked:false, noteId:noteId}, function(json){
                    $.each(taskList[taskId].notes, function(i, item) {
                        if (item.id==noteId) {
                            taskList[taskId].notes[i].checked=true;
                            $('#note_'+noteId).removeClass('note-done').addClass('note-undone');
                            return;
                        }
                    });
                });
            }
        })

        $('#editTitle').focus(function(e){
            $('#isTitleDirty').val(1);
        });

		$("#editTitle").autocomplete(_mtt.tagSuggestUrl, {scroll: false, multiple: false, selectFirst:false, max:8});
        $("#task").autocomplete(_mtt.tagSuggestUrl, {scroll: false, multiple: false, selectFirst:false, max:8});

		$('#taskedit_form').find('select,input,textarea').bind('change keypress', function(){
			flag.editFormChanged = true;
		});

		$('#tasklist .task-through').live('dblclick', function(){
            doubleClickToEdit(this);
		});
        $('#tasklist .task-note-block').live('dblclick', function(){
            doubleClickToEdit(this);
        });

		$('#tasklist .taskactionbtn').live('mouseover', function(){
			var id = parseInt(getLiTaskId(this));
			if(id) taskContextMenu(this, id);
			return false;
		});

		$('#tasklist input[type=checkbox]').live('click', function(){
			var id = parseInt(getLiTaskId(this));
			if(id) completeTask(id, this);
			//return false;
		});

        $('#tasklist .task-toggle').live('click', function(){
			var id = getLiTaskId(this);
			if(id) $('#taskrow_'+id).toggleClass('task-expanded');
			return false;
		});

		$('#tasklist .tag').live('click', function(event){
            if ($(this).hasClass("addTag")) {
                var id = getLiTaskId(this);
                $("#addTag-dialog-form-id").val(id);
                addTagDialog('addTag');
                return false;
            }
			clearTimeout(_mtt.timers.previewtag);
			$('#tasklist li').removeClass('not-in-tagpreview');
			addFilterTag($(this).attr('tag'), $(this).attr('tagid'), (event.metaKey || event.ctrlKey ? true : false) );
			return false;
		});

		if(!this.options.touchDevice) {
			$('#tasklist .task-prio').live('mouseover mouseout', function(event){
				var id = parseInt(getLiTaskId(this));
				if(!id) return;
				if(event.type == 'mouseover') prioPopup(1, this, id);
				else prioPopup(0, this);
			});
		}

		if(this.options.tagPreview) {
			$('#tasklist .tag').live('mouseover mouseout', function(event){
                if ($(this).hasClass("addTag")) return false;
				var reverse = (event.metaKey || event.ctrlKey);
				if(event.type == 'mouseover') {
					_mtt.timers.previewtag = setTimeout( function(){
                        $.each($('#tasklist li'), function(i, item) {
                            var tagLink = $(item).find('a.tag')
                            if (('@'+$(tagLink).attr('tag').toLowerCase() != $(tagLink).html().toLowerCase() && !reverse) || ('@'+$(tagLink).attr('tag').toLowerCase() == $(tagLink).html().toLowerCase() && reverse)) {
                                $(item).addClass('not-in-tagpreview');
                            }
                        })
                    }, _mtt.options.tagPreviewDelay);
				}
				else {
					clearTimeout(_mtt.timers.previewtag);
					$('#tasklist li').removeClass('not-in-tagpreview');
				}
			});
		}

		$("#tasklist").sortable({
				items:'> :not(.task-completed)', cancel:'span,input,a,textarea',
		 		delay:150, start:sortStart, update:orderChanged,
				placeholder:'mtt-task-placeholder',opacity:0.5
		});

		//$("#lists ul").sortable({delay:150, update:listOrderChanged,opacity:0.5,tolerance:'pointer'});
        this.applySingletab();

		// AJAX Errors
		$('#msg').ajaxSend(function(r,s){
			$("#msg").hide().removeClass('mtt-error mtt-info').find('.msg-details').hide();
			$("#loading").show();
		});

		$('#msg').ajaxStop(function(r,s){
			$("#loading").fadeOut();
		});

		$('#msg').ajaxError(function(event, request, settings){
			var errtxt;
			if(request.status == 0) errtxt = 'Bad connection';
			else if(request.status != 200) errtxt = 'HTTP: '+request.status+'/'+request.statusText;
			else errtxt = request.responseText;
			flashError(_mtt.lang.get('error'), errtxt); 
		}); 


		// Error Message details
		$("#msg>.msg-text").click(function(){
			$("#msg>.msg-details").toggle();
		});


		// Authorization
		$('#bar_login').click(function(){
			showAuth(this);
			return false;
		});

		$('#bar_logout').click(function(){
			logout();
			return false;
		});

		$('#login_form').submit(function(){
			doAuth(this);
			return false;
		});


		// Settings
		$("#settings").click(showSettings);
		$("#settings_form").live('submit', function() {
			saveSettings(this);
			return false;
		});
		
		$(".mtt-back-button").live('click', function(){ _mtt.pageBack(); this.blur(); $('#settings').show(); return false } );

		$(window).bind('beforeunload', function() {
			if(_mtt.pages.current.page == 'taskedit' && flag.editFormChanged) {
				return _mtt.lang.get('confirmLeave');
			}
		});


		// tab menu
		this.addAction('listSelected', tabmenuOnListSelected);

		// task context menu
		this.addAction('listsLoaded', cmenuOnListsLoaded);
		this.addAction('listRenamed', cmenuOnListRenamed);
		this.addAction('listAdded', cmenuOnListAdded);
		this.addAction('listSelected', cmenuOnListSelected);
		this.addAction('listOrderChanged', cmenuOnListOrderChanged);
		this.addAction('listHidden', cmenuOnListHidden);

		// select list menu
		this.addAction('listsLoaded', slmenuOnListsLoaded);
		this.addAction('listRenamed', slmenuOnListRenamed);
		this.addAction('listAdded', slmenuOnListAdded);
		this.addAction('listSelected', slmenuOnListSelected);
		this.addAction('listHidden', slmenuOnListHidden);
        //
        $( "#singleInput-dialog-form" ).dialog({
            autoOpen: false,
            minHeight: 0,
            width:270,
            modal: true,
            resizable: false,
            open: function(event, ui) {
                $(event.target).parent().css('top', '150px');
            },
            buttons: [
                {text:_mtt.lang.get('actionSave'), click:function() {
                        if ($("#singleInput-dialog-form-name").val().trim().length > 0) {
                            if ($("#singleInput-dialog-form-fn").val() == 'addList')
                                addList($("#singleInput-dialog-form-name").val());
                            else if ($("#singleInput-dialog-form-fn").val() == 'addProject')
                                addProject($("#singleInput-dialog-form-name").val());
                        }
                        $(this).dialog("close")
                        return false;
                    }
                },
                {text:_mtt.lang.get('actionCancel'), click: function() {
                    $( this ).dialog( "close" );
                }}
            ],
            close: function() {
            }
        });
        // enable enter to submit
        $('#singleInput-dialog-form form').submit(function () {
            $('#singleInput-dialog-form').parent().find('button').first().trigger('click');
            return false;
        });

        $( "#addTag-dialog-form" ).dialog({
            autoOpen: false,
            minHeight: 0,
            width:270,
            modal: true,
            resizable: false,
            open: function(event, ui) {
                $(event.target).parent().css('top', '150px');
            },
            buttons: [
                {text:_mtt.lang.get('actionSave'), click:function() {
                    if ($("#addTag-dialog-form-name").val().trim().length > 0) {
                        addTagToTask($("#addTag-dialog-form-id").val(), $("#addTag-dialog-form-name").val());
                    }
                    $(this).dialog("close");
                    return false;
                }
                },
                {text:_mtt.lang.get('actionCancel'), click: function() {
                    $( this ).dialog( "close" );
                }}
            ],
            close: function() {
                $(".ac_results").hide();
                //$(".ac_results").html('');
            }
        });
        // enable enter to submit
        $('#addTag-dialog-form form').submit(function () {
            $('#addTag-dialog-form').parent().find('button').first().trigger('click');
            return false;
        });
        // enable autocomplete
        $("#addTag-dialog-form-name").autocomplete(_mtt.tagSuggestUrl, {scroll: false, multiple: false, selectFirst:false, max:8});

        $( "#deleteConfirm-dialog-form" ).dialog({
            autoOpen: false,
            minHeight: 0,
            width:270,
            modal: true,
            resizable: false,
            open: function(event, ui) {
                $(event.target).parent().css('top', '150px');
            },
            buttons: [
                {text:_mtt.lang.get('actionConfirm'), click:function() {
                    var fn = $("#deleteConfirm-dialog-form-fn").val();
                    var arg = $("#deleteConfirm-dialog-form-id").val();
                    if (fn=='deleteCurList') deleteCurList();
                    else if (fn=='deleteTask') deleteTask(arg);
                    else if (fn=='clearCompleted') clearCompleted();
                    else if (fn=='deleteInvitation') _mtt.deleteInvitation(arg);
                    else if (fn=='deleteMember') _mtt.deleteMember(arg);
                    $(this).dialog( "close" );}
                },
                {text:_mtt.lang.get('actionCancel'), click: function() {
                    $( this ).dialog( "close" );
                }}
            ],
            close: function() {
            }
        });

		return this;
	},

	log: function(v)
	{
		console.log.apply(this, arguments);
	},

	addAction: function(action, proc)
	{
		if(!this.actions[action]) this.actions[action] = new Array();
		this.actions[action].push(proc);
	},

	doAction: function(action, opts)
	{
		if(!this.actions[action]) return;
		for(var i in this.actions[action]) {
			this.actions[action][i](opts);
		}
	},

	setOptions: function(opts) {
		jQuery.extend(this.options, opts);
	},

    loadProject: function(projectId) {
        /* if edit task is open, close it first */
        if(_mtt.pages.current.page == 'taskedit') {
            $('.mtt-back-button').click();
        }
        _mtt.project = projectId;
        if ($('#project_'+_mtt.project+'_WRITE').length > 0) {
            $('#mtt_body h2').html($('#project_'+_mtt.project+'_WRITE').html());
            $('#settings').hide();
        } else if ($('#project_'+_mtt.project+'_READ').length > 0) {
            $('#mtt_body h2').html($('#project_'+_mtt.project+'_READ').html());
            $('#settings').hide();
        } else if ($('#project_'+_mtt.project+"_OWN").length > 0) {
            $('#mtt_body h2').html($('#project_'+_mtt.project+'_OWN').html());
            $('#settings').show();
        }
        // refresh autocomplete params
        $("#addTag-dialog-form-name").flushCache().setOptions({extraParams:{projectId:_mtt.project}});
        $("#editTitle").flushCache().setOptions({extraParams:{projectId:_mtt.project}});
        $("#task").flushCache().setOptions({extraParams:{projectId:_mtt.project}});
        this.loadLists(1);
    },

	loadLists: function(onInit)
	{
		if(filter.search != '') {
			filter.search = '';
			$('#searchbarkeyword').text('');
			$('#searchbar').hide();
		}
		$('#page_tasks').hide();
		$('#tasklist').html('');
		
		tabLists.clear();
		
		this.db.loadLists({project:this.project}, function(res)
		{
			var ti = '';
			var openListId = 0;
			if(res && res.total>1)
			{
				// open required or first non-hidden list
				for(var i=0; i<res.list.length; i++) {
					if(_mtt.options.openList) {
						if(_mtt.options.openList == res.list[i].id) {
							openListId = res.list[i].id;
							break;
						}
					}
					else if(!res.list[i].hidden) {
						openListId = res.list[i].id;
						break;
					}
				}
				
				// open all tasks tab
				if(_mtt.options.openList == -1) openListId = -1;
				
				// or open first if all list are hidden
				if(!openListId) openListId = res.list[0].id;
				
				$.each(res.list, function(i,item){
                    if (item.id == -1) {
                        tabLists.get(-1).sort = item.sort;
                        tabLists.get(-1).notesExpanded = item.notesExpanded;
                        tabLists.get(-1).showCompleted = item.showCompleted;
                    } else {
                        tabLists.add(item);
                        ti += '<li id="list_'+item.id+'" class="mtt-tab'+(item.hidden?' mtt-tabs-hidden':'')+'">'+
                            '<a href="#list/'+item.id+'" title="'+item.name+'"><span>'+item.name+'</span>'+
                            '<div class="list-action"></div></a></li>';
                    }
				});
                checkAllListsTab();
			}
			
			if(openListId) {
				$('#mtt_body').removeClass('no-lists');
				$('.mtt-need-list').removeClass('mtt-item-disabled');
			}
			else {
				curList = 0;
				$('#mtt_body').addClass('no-lists');
				$('.mtt-need-list').addClass('mtt-item-disabled');
			}

			_mtt.options.openList = 0;
			$('#lists ul').html(ti);
			$('#lists').show();
			_mtt.doAction('listsLoaded');
			tabSelect(openListId);

			$('#page_tasks').show();
		});

		if(onInit) updateAccessStatus();
	},

	duedatepickerformat: function()
	{
		if(!this.options.duedatepickerformat) return 'yyyy-mm-dd';
	
		var s = this.options.duedatepickerformat.replace(/(.)/g, function(t,s) {
			switch(t) {
				case 'Y': return 'yy';
				case 'y': return 'y';
				case 'd': return 'dd';
				case 'j': return 'd';
				case 'm': return 'mm';
				case 'n': return 'm';
				case '/':
				case '.':
				case '-': return t;
				default: return '';
			}
		});

		if(s == '') return 'yy-mm-dd';
		return s;
	},

	errorDenied: function()
	{
		flashError(this.lang.get('denied'));
	},
	
	pageSet: function(page, pageClass)
	{
		var prev = this.pages.current;
		prev.lastScrollTop = $(window).scrollTop();
		this.pages.prev.push(this.pages.current);
		this.pages.current = {page:page, pageClass:pageClass};
		showhide($('#page_'+ this.pages.current.page).addClass('mtt-page-'+ this.pages.current.pageClass), $('#page_'+ prev.page));
	},
	
	pageBack: function()
	{
		if(this.pages.current.page == 'tasks') return false;
		var prev = this.pages.current;
		this.pages.current = this.pages.prev.pop();
		showhide($('#page_'+ this.pages.current.page), $('#page_'+ prev.page).removeClass('mtt-page-'+prev.page.pageClass));
		$(window).scrollTop(this.pages.current.lastScrollTop);
	},
	
	applySingletab: function(yesno)
	{
		if(yesno == null) yesno = this.options.singletab;
		else this.options.singletab = yesno;
		
		if(yesno) {
			$('#lists .mtt-tabs').addClass('mtt-tabs-only-one');
			//$("#lists ul").sortable('disable');
		}
		else {
			$('#lists .mtt-tabs').removeClass('mtt-tabs-only-one');
			//$("#lists ul").sortable('enable');
		}
	},
	
	filter: {
		_filters: [],
		clear: function() {
			this._filters = [];
			$('#mtt_filters').html('');
		},
		addTag: function(tagId, tag, exclude)
		{
			for(var i in this._filters) {
				if(this._filters[i].tagId && this._filters[i].tagId == tagId) return false;
			}
			this._filters.push({tagId:tagId, tag:tag, exclude:exclude});
			$('#mtt_filters').append('<span class="tag-filter tag-id-'+tagId+
				(exclude ? ' tag-filter-exclude' : '')+'"><span class="mtt-filter-header">'+
				_mtt.lang.get('tagfilter')+'</span>@'+tag+'<span class="mtt-filter-close" tagid="'+tagId+'"></span></span>');
			return true;
		},
		cancelTag: function(tagId)
		{
			for(var i in this._filters) {
				if(this._filters[i].tagId && this._filters[i].tagId == tagId) {
					this._filters.splice(i,1);
					$('#mtt_filters .tag-filter.tag-id-'+tagId).remove();
					return true;
				}
			}
			return false;
		},
		getTags: function(withExcluded)
		{
			var a = [];
			for(var i in this._filters) {
				if(this._filters[i].tagId) {
					if(this._filters[i].exclude && withExcluded) a.push('^'+ this._filters[i].tag);
					else if(!this._filters[i].exclude) a.push(this._filters[i].tag)
				}
			}
			return a.join(',');
		}
	},
	
	parseAnchor: function()
	{
		if(location.hash == '') return false;
		var h = location.hash.substr(1);
		var a = h.split("/");
		var p = {};
		var s = '';
		
		for(var i=0; i<a.length; i++)
		{
			s = a[i];
			switch(s) {
				case "list": if(a[++i].match(/^-?\d+$/)) { p[s] = a[i]; } break;
				case "alltasks": p.list = '-1'; break;
			}
		}

		if(p.list) this.options.openList = p.list;
		
		return p;
	},

    confirmAction: function(messageKey, fn, id) {
        $("#deleteConfirm-dialog-form label").html(_mtt.lang.get(messageKey));
        $("#deleteConfirm-dialog-form-fn").val(fn);
        $("#deleteConfirm-dialog-form-id").val(id);
        $("#deleteConfirm-dialog-form").dialog( "open" );
    },

    newNotificationCount: function() {
        this.db.newNotificationCount({}, function(json) {
            if (json.total < 1) return;
            $('#notification').append(' <strong>('+json.total+')</strong>');
        });
    }
};

function doubleClickToEdit(elem) {
    //clear selection
    if(document.selection && document.selection.empty && document.selection.createRange().text) document.selection.empty();
    else if(window.getSelection) window.getSelection().removeAllRanges();
    var id = getLiTaskId(elem);
    if(id) editTask(parseInt(id));
}

function addProjectDialog() {
    singleInputDialog('addProject', 'addProject');
}

function addProject(title) {
    _mtt.db.request('addProject', {title:title}, function(json){
        if(!parseInt(json.total)) return;
        var htmlStr=_mtt.lang.get('workspaces') + '&nbsp;&nbsp;&nbsp;';
        $.each(json.list, function(i,item){
            if (item.project.title == title) {
                window.location.href = _mtt.mttUrl + "w/" + item.project.id;
            }
        });
    });
}

function addList(name)
{
	_mtt.db.request('addList', {name:name,project:_mtt.project}, function(json){
		if(!parseInt(json.total)) return;
		var item = json.list[0];
		var i = tabLists.length();
		tabLists.add(item);
		if(i > 0) {
			$('#lists ul').append('<li id="list_'+item.id+'" class="mtt-tab">'+
					'<a href="#" title="'+item.name+'"><span>'+item.name+'</span>'+
					'<div class="list-action"></div></a></li>');
			mytinytodo.doAction('listAdded', item);
		}
		else _mtt.loadLists();
        checkAllListsTab();
	});
};

function addTagToTask(id, tag) {
        _mtt.db.request('addTag', {id:id, tags:tag}, function(json){
            if(!parseInt(json.total)) return;
            var item = json.list[0];
            // now add tag to the task
            $("#taskrow_"+id+" .task-tags").replaceWith(prepareTagsStr(item));
            // add tag-id-# class to li
            $("#taskrow_"+id).attr('class','');
            if (item.tags && item.tags.length > 0) {
                $.each(item.tags, function(i, tag) {
                    $("#taskrow_"+id).addClass("tag-id-"+tag.id);
                })
            }
            // update taskList
            taskList[id]=json.list[0];
        });
};

function renameCurList()
{
	if(!curList) return;
	var r = prompt(_mtt.lang.get('list_rename'), dehtml(curList.name));
	if(r == null || r == '') return;

	_mtt.db.request('renameList', {list:curList.id, name:r}, function(json){
		if(!parseInt(json.total)) return;
		var item = json.list[0];
		curList = item;
		tabLists.replace(item); 
		$('#lists ul>.mtt-tabs-selected>a').attr('title', item.name).find('span').html(item.name);
		mytinytodo.doAction('listRenamed', item);
	});
};

function deleteCurList() {
    if(!curList) return false;
	_mtt.db.request('deleteList', {list:curList.id}, function(json){
		if(!parseInt(json.total)) return;
		_mtt.loadLists();
	})
};

function publishCurList()
{
	if(!curList) return false;
	_mtt.db.request('publishList', { list:curList.id, publish:curList.published?0:1 }, function(json){
		if(!parseInt(json.total)) return;
		curList.published = curList.published?0:1;
		if(curList.published) {
			$('#btnPublish').addClass('mtt-item-checked');
			$('#btnRssFeed').removeClass('mtt-item-disabled');
		}
		else {
			$('#btnPublish').removeClass('mtt-item-checked');
			$('#btnRssFeed').addClass('mtt-item-disabled');
		}
	});
};

function singleInputDialog(messageKey, fn) {
    $("#singleInput-dialog-form label").html(_mtt.lang.get(messageKey));
    $("#singleInput-dialog-form-name").val('');
    $("#singleInput-dialog-form-fn").val(fn);
    $("#singleInput-dialog-form").dialog( "open" );
    $("#singleInput-dialog-form-name").focus();
}

function addTagDialog(messageKey) {
    $("#addTag-dialog-form label").html(_mtt.lang.get(messageKey));
    $("#addTag-dialog-form-name").val('');
    $("#addTag-dialog-form").dialog( "open" );
    $("#addTag-dialog-form-name").focus();
}

function loadTasks(opts)
{
	if(!curList) return false;
	setSort(curList.sort, 1);
	opts = opts || {};
	if(opts.clearTasklist) {
		$('#tasklist').html('');
		$('#total').html('0');
	}

	_mtt.db.request('loadTasks', {
		list: curList.id,
        project: _mtt.project,
        showCompleted: curList.showCompleted,
		sort: curList.sort,
		search: filter.search,
		tag: _mtt.filter.getTags(true),
		setCompl: opts.setCompl
	}, function(json){
		taskList.length = 0;
		taskOrder.length = 0;
		taskCnt.total = taskCnt.past = taskCnt.today = taskCnt.soon = 0;
		var tasks = '';
		$.each(json.list, function(i,item){
			tasks += prepareTaskStr(item);
            item.dateCreatedMillis = getDate(item.dateCreated).getTime();
            item.lastUpdatedMillis = getDate(item.lastUpdated).getTime();
            item.dateDueMillis = getDate(item.dateDue).getTime();
			taskList[item.id] = item;
			taskOrder.push(parseInt(item.id));
			changeTaskCnt(item, 1);
		});
		if(opts.beforeShow && opts.beforeShow.call) {
			opts.beforeShow();
		}
		refreshTaskCnt();
		$('#tasklist').html(tasks);
	});
};


function prepareTaskStr(item, noteExp)
{
	// &mdash; = &#8212; = —
	var id = item.id;
	var prio = item.priority;
	return '<li id="taskrow_'+id+'" class="' + prepareItemStyleClass(item, noteExp) + '">' +
		'<div class="task-actions"><a href="#" class="taskactionbtn"></a></div>'+"\n"+
		'<div class="task-left"><div class="task-toggle"></div>'+
		'<input type="checkbox" '+(flag.readOnly?'disabled="disabled"':'')+(item.completed?'checked="checked"':'')+'/></div>'+"\n"+
		'<div class="task-middle"><div class="task-through-right">'+prepareDuedate(item)+
		'<span class="task-date-completed"><span title="'+item.dateInlineTitle+'">'+item.dateInline+'</span>&#8212;'+
		'<span title="'+item.dateCompletedInlineTitle+'">'+item.dateCompletedInline+'</span></span></div>'+"\n"+
		'<div class="task-through">'+preparePrio(prio,id)+'<span class="task-title">'+prepareHtml(item.title)+'</span> '+
		(curList.id == -1 ? '<span class="task-listname">'+ tabLists.get(item.toDoListId).name +'</span>' : '') +
		prepareTagsStr(item)+'</div>'+'<div class="task-note-block">'+
			'<div id="tasknote'+id+'" class="task-note">'+prepareNotes(item.notes)+'</div>'+
		'</div>'+
		"</div></li>\n";
};

function prepareNotes(notes) {
    var s = "";
    if (notes) {
        for (var i in notes) {
            var noteHtml = prepareHtml(notes[i].content);
            s += '<div id="note_'+notes[i].id+'" class="note-'+(notes[i].checked?'done':'undone')+'"><span>'+noteHtml+'</span><a href="#" class="weak note-done-action">Done</a><a href="#" class="weak note-undone-action">Undone</a></div>';
        }
    }
    return s;
};

function prepareNotesInFrame(notes) {
    var s = "";
    if (notes) {
        for (var i in notes) {
            s += '<div>'+(notes[i].checked?'<strike>'+notes[i].content+'</strike>':notes[i].content)+'</div>';
        }
    }
    return s;
};

function prepareHtml(s)
{
    // escape html tags
    if (s) {
        s = $("<div/>").text(s).html();
        // make URLs clickable
        s = s.replace(/(^|\s|>)(www\.([\w\#$%&~\/.\-\+;:=,\?\[\]@]+?))(,|\.|:|)?(?=\s|&quot;|&lt;|&gt;|\"|<|>|$)/gi, '$1<a href="http://$2" target="_blank">$2</a>$4');
        return s.replace(/(^|\s|>)((?:http|https|ftp):\/\/([\w\#$%&~\/.\-\+;:=,\?\[\]@]+?))(,|\.|:|)?(?=\s|&quot;|&lt;|&gt;|\"|<|>|$)/ig, '$1<a href="$2" target="_blank">$2</a>$4');
    } else {
        return '';
    }
};

function preparePrio(prio,id)
{
	var cl =''; var v = '';
	if(prio < 0) { cl = 'prio-neg prio-neg-'+Math.abs(prio); v = '&#8722;'+Math.abs(prio); }	// &#8722; = &minus; = −
	else if(prio > 0) { cl = 'prio-pos prio-pos-'+prio; v = '+'+prio; }
	else { cl = 'prio-zero'; v = '&#177;0'; }													// &#177; = &plusmn; = ±
	return '<span class="task-prio '+cl+'">'+v+'</span>';
};

function prepareTagsStr(item)
{
    if (!item.tags || item.tags.length == 0) return '<span class="task-tags"><a href="#" class="tag addTag">'+_mtt.lang.get('a_addTag')+'</a></span>';
    var a = [];
    var b = [];
    for (var i in item.tags) {
        a.push(item.tags[i].text);
        b.push(item.tags[i].id);
    }
	for(var i in a) {
		a[i] = '<a href="#" class="tag" tag="'+a[i]+'" tagid="'+b[i]+'">@'+a[i]+'</a>';
	}
	return '<span class="task-tags">'+a.join(', ')+' <a href="#" class="tag addTag">'+_mtt.lang.get('a_addTag')+'</a></span>';
};

function prepareTagsClass(tags)
{
	if(!tags) return '';
    var a = [];
    for (var i in tags) {
        a.push(tags[i].id);
    }
	if(!a.length) return '';
	for(var i in a) {
		a[i] = 'tag-id-'+a[i];
	}
	return ' '+a.join(' ');
};

function prepareDuedate(item)
{
	if(!item.dateDueInDays) return '';
	return '<span class="duedate" title="'+item.dueTitle+'"><span class="duedate-arrow">→</span> due in '+ Math.ceil(item.dateDueInDays) +' days</span>';
};

function prepareItemStyleClass(item, noteExp) {
    var styleClass = item.completed?'task-completed ':'';
    if (item.dateDueInDays) {
        if (item.dateDueInDays <= -1) {
            styleClass += "past";
        } else if (item.dateDueInDays < 2) {
            styleClass += "today";
        }
    }
    styleClass += (item.notes && item.notes.length > 0?' task-has-note':'') +
        ((curList.notesExpanded && item.notes && item.notes.length > 0) || noteExp ? ' task-expanded' : '') +
        prepareTagsClass(item.tags);
    return styleClass;
}

function submitNewTask(form)
{
	if(form.task.value == '') return false;
	_mtt.db.request('newTask', { list:curList.id, title: form.task.value, tag:_mtt.filter.getTags() }, function(json){
		if(!json.total) return;
		$('#total').text( parseInt($('#total').text()) + 1 );
		taskCnt.total++;
		form.task.value = '';
		var item = json.list[0];
		taskList[item.id] = item;
		taskOrder.push(parseInt(item.id));
		$('#tasklist').append(prepareTaskStr(item));
		changeTaskOrder(item.id);
		$('#taskrow_'+item.id).effect("highlight", {color:_mtt.theme.newTaskFlashColor}, 2000);
		refreshTaskCnt();
	}); 
	flag.tagsChanged = true;
	return false;
};


function changeTaskOrder(id)
{
	id = parseInt(id);
	if(taskOrder.length < 2) return;
	var oldOrder = taskOrder.slice();
    var direction = curList.sort && curList.sort.match(/DESC$/) ? -1 : 1;
	// sortByHand
	if(!curList.sort || curList.sort == "DEFAULT") taskOrder.sort( function(a,b){
			if(taskList[a].completed != taskList[b].completed) return taskList[a].completed-taskList[b].completed;
			return taskList[a].orderIndex-taskList[b].orderIndex
		});
	// sortByPrio
	else if(curList.sort.match(/^PRIORITY/)) taskOrder.sort( function(a,b){
			if(taskList[a].completed != taskList[b].completed) return taskList[a].completed-taskList[b].completed;
			if(taskList[a].priority != taskList[b].priority) return (taskList[b].priority-taskList[a].priority) * direction;
			if(taskList[a].dateDueMillis != taskList[b].dateDueMillis) return taskList[a].dateDueMillis-taskList[b].dateDueMillis;
			return taskList[a].orderIndex-taskList[b].orderIndex;
		});
	// sortByDueDate
	else if(curList.sort.match(/^DUE_DATE/)) taskOrder.sort( function(a,b){
			if(taskList[a].completed != taskList[b].completed) return taskList[a].completed-taskList[b].completed;
			if(taskList[a].dateDueMillis != taskList[b].dateDueMillis) return (taskList[a].dateDueMillis-taskList[b].dateDueMillis) * direction;
			if(taskList[a].priority != taskList[b].priority) return taskList[b].priority-taskList[a].priority;
			return taskList[a].orderIndex-taskList[b].orderIndex;
		});
	// sortByDateCreated
	else if(curList.sort.match(/^DATE_CREATED/)) taskOrder.sort( function(a,b){
			if(taskList[a].completed != taskList[b].completed) return taskList[a].completed-taskList[b].completed;
			if(taskList[a].dateCreatedMillis != taskList[b].dateCreatedMillis) return (taskList[a].dateCreatedMillis-taskList[b].dateCreatedMillis) * direction;
			if(taskList[a].priority != taskList[b].priority) return taskList[b].priority-taskList[a].priority;
			return taskList[a].orderIndex-taskList[b].orderIndex;
		});
	// sortByDateModified
	else if(curList.sort.match(/^LAST_UPDATED/)) taskOrder.sort( function(a,b){
			if(taskList[a].completed != taskList[b].completed) return taskList[a].completed-taskList[b].completed;
			if(taskList[a].lastUpdatedMillis != taskList[b].lastUpdatedMillis) return (taskList[a].lastUpdatedMillis-taskList[b].lastUpdatedMillis) * direction;
			if(taskList[a].priority != taskList[b].priority) return taskList[b].priority-taskList[a].priority;
			return taskList[a].orderIndex-taskList[b].orderIndex;
		});
	else return;
	if(oldOrder.toString() == taskOrder.toString()) return;
	if(id && taskList[id])
	{
		// optimization: determine where to insert task: top or after some task
		var indx = $.inArray(id,taskOrder);
		if(indx ==0) {
			$('#tasklist').prepend($('#taskrow_'+id))
		} else {
			var after = taskOrder[indx-1];
			$('#taskrow_'+after).after($('#taskrow_'+id));
		}
	}
	else {
		var o = $('#tasklist');
		for(var i in taskOrder) {
			o.append($('#taskrow_'+taskOrder[i]));
		}
	}
};


function prioPopup(act, el, id)
{
	if(act == 0) {
		clearTimeout(objPrio.timer);
		return;
	}
	var offset = $(el).offset();
    var prioOffset = id && taskList[id] ? -(taskList[id].priority + 1) * 21 : 1;
    $('#priopopup').css({ position: 'absolute', top: offset.top+2, left: offset.left+prioOffset-4 });
	objPrio.taskId = id;
	objPrio.el = el;
	objPrio.timer = setTimeout("$('#priopopup').show()", 300);
};

function prioClick(prio, el)
{
	el.blur();
	prio = parseInt(prio);
	$('#priopopup').fadeOut('fast'); //.hide();
	setTaskPrio(objPrio.taskId, prio);
};

function setTaskPrio(id, prio)
{
	_mtt.db.request('setPrio', {id:id, prio:prio});
	taskList[id].priority = prio;
	var $t = $('#taskrow_'+id);
	$t.find('.task-prio').replaceWith(preparePrio(prio, id));
	if(curList.sort != "DEFAULT") changeTaskOrder(id);
	$t.effect("highlight", {color:_mtt.theme.editTaskFlashColor}, 'normal');
};

function setSort(v, init)
{
	$('#listmenucontainer .sort-item').removeClass('mtt-item-checked').children('.mtt-sort-direction').text('');
	if(v == "DEFAULT") $('#sortByHand').addClass('mtt-item-checked');
	else if(v=="PRIORITY_DESC") $('#sortByPrio').addClass('mtt-item-checked').children('.mtt-sort-direction').text('↓');
    else if(v=="PRIORITY_ASC") $('#sortByPrio').addClass('mtt-item-checked').children('.mtt-sort-direction').text('↑');
	else if(v=="DUE_DATE_DESC") $('#sortByDueDate').addClass('mtt-item-checked').children('.mtt-sort-direction').text('↓');
    else if(v=="DUE_DATE_ASC") $('#sortByDueDate').addClass('mtt-item-checked').children('.mtt-sort-direction').text('↑');
	else if(v=="DATE_CREATED_DESC") $('#sortByDateCreated').addClass('mtt-item-checked').children('.mtt-sort-direction').text('↓');
    else if(v=="DATE_CREATED_ASC") $('#sortByDateCreated').addClass('mtt-item-checked').children('.mtt-sort-direction').text('↑');
	else if(v=="LAST_UPDATED_DESC") $('#sortByDateModified').addClass('mtt-item-checked').children('.mtt-sort-direction').text('↓');
    else if(v=="LAST_UPDATED_ASC") $('#sortByDateModified').addClass('mtt-item-checked').children('.mtt-sort-direction').text('↑');
	else return;

	curList.sort = v;
	if(v == "DEFAULT" && !flag.readOnly)
        $("#tasklist").sortable('enable');
	else
        $("#tasklist").sortable('disable');
	
	if(!init)
	{
		changeTaskOrder();
		if(!flag.readOnly) _mtt.db.request('setSort', {list:curList.id, sort:curList.sort, project:_mtt.project});
	}
};

function changeTaskCnt(task, dir, old)
{
	if(dir > 0) dir = 1;
	else if(dir < 0) dir = -1;
	if(dir == 0 && old != null && task.dateDueInDays != old.dateDueInDays) //on saveTask
	{
        changeTaskCntByDueDate(old, -1);
        changeTaskCntByDueDate(task, 1);
	}
	else if(dir == 0 && old == null) //on comleteTask
	{
		if(!curList.showCompleted && task.completed) taskCnt.total--;
        changeTaskCntByDueDate(task, task.completed ? -1 : 1);
	}
	if(dir != 0) {
		if(!task.completed) changeTaskCntByDueDate(task, dir);
		taskCnt.total += dir;
	}
};

function changeTaskCntByDueDate(task, dir) {
    if (!task.dateDueInDays) {
        return;
    } else if (task.dateDueInDays <= 0) {
        taskCnt['past']+=dir;
    } else if (task.dateDueInDays < 2) {
        taskCnt['today']+=dir;
    } else if (task.dateDueInDays < 8) {
        taskCnt['soon']+=dir;
    }
}

function refreshTaskCnt()
{
	$('#cnt_total').text(taskCnt.total);
	$('#cnt_past').text(taskCnt.past);
	$('#cnt_today').text(taskCnt.today);
	$('#cnt_soon').text(taskCnt.soon);
	if(filter.due == '') $('#total').text(taskCnt.total);
	else if(taskCnt[filter.due] != null) $('#total').text(taskCnt[filter.due]);
};


function setTaskview(v)
{
	if(v == 0)
	{
		if(filter.due == '') return;
		$('#taskview .btnstr').text(_mtt.lang.get('tasks'));
		$('#tasklist').removeClass('filter-'+filter.due);
		filter.due = '';
		$('#total').text(taskCnt.total);
	}
	else if(v=='past' || v=='today' || v=='soon')
	{
		if(filter.due == v) return;
		else if(filter.due != '') {
			$('#tasklist').removeClass('filter-'+filter.due);
		}
		$('#tasklist').addClass('filter-'+v);
		$('#taskview .btnstr').text(_mtt.lang.get('f_'+v));
		$('#total').text(taskCnt[v]);
		filter.due = v;
	}
};

function toggleWatch() {
    var show = !curList.watchedByMe;
    curList.watchedByMe = show;
    if(show) $('#btnWatch').addClass('mtt-item-checked');
    else $('#btnWatch').removeClass('mtt-item-checked');
    _mtt.db.request('setWatchList', {list:curList.id, watchedByMe:curList.watchedByMe}, function(json){});
};

function toggleAllNotes()
{
    var show = !curList.notesExpanded;
	for(var id in taskList)
	{
		if(!taskList[id].notes || taskList[id].notes.length == 0) continue;
		if(show) $('#taskrow_'+id).addClass('task-expanded');
		else $('#taskrow_'+id).removeClass('task-expanded');
	}
	curList.notesExpanded = show;
    if(show) $('#btnExpandNotes').addClass('mtt-item-checked');
    else $('#btnExpandNotes').removeClass('mtt-item-checked');
	_mtt.db.request('setShowNotesInList', {list:curList.id, notesExpanded:curList.notesExpanded}, function(json){});
};

function tabSelect(elementOrId)
{
	var id;
	if(typeof elementOrId == 'number') id = elementOrId;
	else if(typeof elementOrId == 'string') id = parseInt(elementOrId);
	else {
		id = $(elementOrId).attr('id');
		if(!id) return;
		id = parseInt(id.split('_', 2)[1]);
	}
	if(!tabLists.exists(id)) return;
	$('#lists .mtt-tabs-selected').removeClass('mtt-tabs-selected');
	$('#list_all').removeClass('mtt-tabs-selected');
	
	if(id == -1) {
		$('#list_all').addClass('mtt-tabs-selected').removeClass('mtt-tabs-hidden');
		$('#listmenucontainer .mtt-need-real-list').addClass('mtt-item-hidden');
	}
	else {
		$('#list_'+id).addClass('mtt-tabs-selected').removeClass('mtt-tabs-hidden');
		$('#listmenucontainer .mtt-need-real-list').removeClass('mtt-item-hidden');
	}
	
	if(curList.id != id)
	{
		if(id == -1) $('#mtt_body').addClass('show-all-tasks');
		else $('#mtt_body').removeClass('show-all-tasks');
		if(filter.search != '') liveSearchToggle(0, 1);
		mytinytodo.doAction('listSelected', tabLists.get(id));
	}
	curList = tabLists.get(id);
	if(curList.hidden) {
		curList.hidden = false;
		if(curList.id > 0) _mtt.db.request('setHideList', {list:curList.id, hide:0});
	}
	flag.tagsChanged = true;
	cancelTagFilter(0, 1);
	setTaskview(0);
	loadTasks({clearTasklist:1});
};



function listMenu(el)
{
	if(!mytinytodo.menus.listMenu) mytinytodo.menus.listMenu = new mttMenu('listmenucontainer', {onclick:listMenuClick});
	mytinytodo.menus.listMenu.show(el);
};

function listMenuClick(el, menu)
{
	if(!el.id) return;
	switch(el.id) {
		case 'btnRenameList': renameCurList(); break;
		case 'btnDeleteList': _mtt.confirmAction('deleteList', 'deleteCurList', ''); break;
		case 'btnPublish': publishCurList(); break;
		case 'btnExportCSV': exportCurList('csv'); break;
		case 'btnExportICAL': exportCurList('ical'); break;
		case 'btnRssFeed': feedCurList(); break;
        case 'btnWatch': toggleWatch(); break;
		case 'btnShowCompleted': showCompletedToggle(); break;
        case 'btnExpandNotes': toggleAllNotes(); break;
		case 'btnClearCompleted': _mtt.confirmAction('clearCompleted', 'clearCompleted', ''); break;
		case 'sortByHand': setSort("DEFAULT"); break;
		case 'sortByPrio': setSort(curList.sort=='PRIORITY_DESC' ? 'PRIORITY_ASC' : 'PRIORITY_DESC'); break;
		case 'sortByDueDate': setSort(curList.sort=='DUE_DATE_DESC' ? 'DUE_DATE_ASC' : 'DUE_DATE_DESC'); break;
		case 'sortByDateCreated': setSort(curList.sort=='DATE_CREATED_DESC' ? 'DATE_CREATED_ASC' : 'DATE_CREATED_DESC'); break;
		case 'sortByDateModified': setSort(curList.sort=='LAST_UPDATED_DESC' ? 'LAST_UPDATED_ASC' : 'LAST_UPDATED_DESC'); break;
	}
};

function deleteTask(id)
{
	_mtt.db.request('deleteTask', {id:id}, function(json){
		if(!parseInt(json.total)) return;
		taskOrder.splice($.inArray(id,taskOrder), 1);
		$('#taskrow_'+id).fadeOut('normal', function(){ $(this).remove() });
		changeTaskCnt(taskList[id], -1);
		refreshTaskCnt();
		delete taskList[id];
	});
	flag.tagsChanged = true;
	return false;
};

function completeTask(id, ch)
{
	if(!taskList[id]) return; //click on already removed from the list while anim. effect
	var completed = false;
	if(ch.checked) completed = true;
	_mtt.db.request('completeTask', {id:id, completed:completed, list:curList.id}, function(json){
		if(!parseInt(json.total)) return;
		var item = json.list[0];
		if(item.completed) $('#taskrow_'+id).addClass('task-completed');
		else $('#taskrow_'+id).removeClass('task-completed');
		taskList[id] = item;
		changeTaskCnt(taskList[id], 0);
		if(item.completed && !curList.showCompleted) {
			delete taskList[id];
			taskOrder.splice($.inArray(id,taskOrder), 1);
			$('#taskrow_'+id).fadeOut('normal', function(){ $(this).remove() });
		}
		else if(curList.showCompleted) {
			$('#taskrow_'+item.id).replaceWith(prepareTaskStr(taskList[id]));
			$('#taskrow_'+id).fadeOut('fast', function(){	
				changeTaskOrder(id);				
				$(this).effect("highlight", {color:_mtt.theme.editTaskFlashColor}, 'normal', function(){$(this).css('display','')});
			});
		}
		refreshTaskCnt();
	});
	return false;
};

function editTask(id)
{
    _mtt.db.request('loadTask', {id:id}, function(json){
        if(!parseInt(json.total)) return;
        taskList[id] = json.list[0];
        var item = taskList[id];
        if(!item) return false;
        // no need to clear form
        var form = document.getElementById('taskedit_form');
        form.task.value = dehtml(item.title) + (item.tags ? $(item.tags).map(function() { return ' @'+this.text }).get().join('') : '');
        if (item.notes) {
            for (var i in item.notes) {
                $('.form-row input.note').parent().append('<input type="text" name="note" class="in500 note"/>');
            }
            $.each($('.form-row input.note'), function(i,elem){
                if (i<item.notes.length) {
                    $(elem).val(item.notes[i].content);
                }});
        }
        form.id.value = item.id;
        form.duedate.value = item.dateDue ? $.datepicker.formatDate(_mtt.duedatepickerformat(), getDate(item.dateDue)) : '';
        form.prio.value = item.priority;
        $('#taskedit-date .date-created>span').text(item.dateCreated);
        if(item.completed) $('#taskedit-date .date-completed').show().find('span').text(item.dateCompleted);
        else $('#taskedit-date .date-completed').hide();
        $('#noteFrame').css('height', (item.notes&&item.notes.length>3 ? 18*item.notes.length : 60)+'px');
        writeToNoteFrame(prepareNotesInFrame(item.notes));
        showEditForm();
    });
	return false;
};

    function cloneTask(id)
    {
        var item = taskList[id];
        if(!item) return false;

        _mtt.db.request('cloneTask', {id:id},
            function(json){
                loadTasks();
            });
        return false;
    };

function clearEditForm()
{
	var form = document.getElementById('taskedit_form');
	form.task.value = '';
	form.note.value = '';
	form.duedate.value = '';
	form.prio.value = '0';
	form.id.value = '';
    writeToNoteFrame('');
};

function showEditForm(isAdd)
{
	var form = document.getElementById('taskedit_form');
    form.isNoteDirty.value = 0;
    form.isTitleDirty.value = 0;
    // enable iframe edit
    var d = getNoteFrameDocument();
    if (d) {
        d.designMode="On";
        $(d).unbind('keydown'); // so binding works repeatedly
        $(d).bind('keydown', function(e) {
            form.isNoteDirty.value=1;
            var code= (e.keyCode ? e.keyCode : e.which);
            if (code == 13 && d.queryCommandValue('strikethrough')=='true') {
                d.execCommand('strikethrough', false, null);
            }
        })
        $(d).unbind('contextmenu'); // so binding works repeatedly
        $(d).bind('contextmenu', function() {
            form.isNoteDirty.value=1;
        })
    }
	if(isAdd)
	{
		$('#page_taskedit').removeClass('mtt-inedit').addClass('mtt-inadd');
		form.isadd.value = 1;
		if($('#task').val() != '')
		{
            form.task.value = $('#task').val();
            $('#task').val('');
		}
	}
	else {
		$('#page_taskedit').removeClass('mtt-inadd').addClass('mtt-inedit');
		form.isadd.value = 0;
	}

	flag.editFormChanged = false;
	_mtt.pageSet('taskedit');
};

function loadTags(listId, callback)
{
	_mtt.db.request('tagCloud', {list:listId,project:_mtt.project}, function(json){
		if(!parseInt(json.total)) tagsList = [];
		else tagsList = json.list;
		var cloud = '';
		$.each(tagsList, function(i,item){
			cloud += ' <a href="#" tag="'+item.text+'" tagid="'+item.id+'" class="tag w'+item.weight+'" >@'+item.text+' ('+item.weight+')</a>';
		});
		$('#tagcloudcontent').html(cloud)
		flag.tagsChanged = false;
		callback();
	});
};

function cancelTagFilter(tagId, dontLoadTasks)
{
	if(tagId)  _mtt.filter.cancelTag(tagId);
	else _mtt.filter.clear();
	if(dontLoadTasks==null || !dontLoadTasks) loadTasks();
};

function addFilterTag(tag, tagId, exclude)
{
	if(!_mtt.filter.addTag(tagId, tag, exclude)) return false;
	loadTasks();
};

function liveSearchToggle(toSearch, dontLoad)
{
	if(toSearch)
	{
		$('#search').focus();
	}
	else
	{
		if($('#search').val() != '') {
			filter.search = '';
			$('#search').val('');
			$('#searchbarkeyword').text('');
			$('#searchbar').hide();
			$('#search_close').hide();
			if(!dontLoad) loadTasks();
		}
		
		$('#search').blur();
	}
};

function searchTasks(force)
{
	var newkeyword = $('#search').val();
	if(newkeyword == filter.search && !force) return false;
	filter.search = newkeyword;
	$('#searchbarkeyword').text(filter.search);
	if(filter.search != '') $('#searchbar').fadeIn('fast');
	else $('#searchbar').fadeOut('fast');
	loadTasks();
	return false;
};

function saveTask(form)
{
	if(flag.readOnly) return false;
    var param = { list:curList.id, prio:form.prio.value, duedate:form.duedate.value };
    var isTitleDirty = form.isTitleDirty.value != 0;
    if (isTitleDirty) {
        param['title'] = form.task.value;
    }
    var isNoteDirty = form.isNoteDirty.value != 0;
    if (isNoteDirty) {
        var d = getNoteFrameDocument();
        if (d) form.note.value = $('body', d).html();
        param['note'] = form.note.value;
    }
    var isadd = form.isadd.value != 0;
    if (!isadd) {
        param['taskId'] = form.id.value;
    }
	_mtt.db.request('saveFullTask', param, function(json){
		if(!parseInt(json.total)) return;
		var item = json.list[0];
        if (isadd) {
            changeTaskCnt(item, 1);
        } else {
            changeTaskCnt(item, 0, taskList[item.id]);
            //reuse client cache
            if (!isNoteDirty) item.notes = taskList[item.id].notes;
            if (!isTitleDirty) item.tags = taskList[item.id].tags;
        }
		taskList[item.id] = item;
        if (isadd) {
            taskOrder.push(parseInt(item.id));
            $('#tasklist').append(prepareTaskStr(item));
            changeTaskOrder(item.id);
        } else {
            var noteExpanded = (item.notes != null && item.notes.length > 0 && $('#taskrow_'+item.id).is('.task-expanded')) ? 1 : 0;
            $('#taskrow_'+item.id).replaceWith(prepareTaskStr(item, noteExpanded));
            if(curList.sort != "DEFAULT") changeTaskOrder(item.id);
        }
		_mtt.pageBack();

		refreshTaskCnt();
        if (isadd) {
            $('#taskrow_'+item.id).effect("highlight", {color:_mtt.theme.newTaskFlashColor}, 2000);
        } else {
            $('#taskrow_'+item.id).effect("highlight", {color:_mtt.theme.editTaskFlashColor}, 'normal', function(){$(this).css('display','')});
        }
	});

	flag.tagsChanged = true;
	return false;
};


function sortStart(event,ui)
{
	// remember initial order before sorting
	sortOrder = $(this).sortable('toArray');
};

function orderChanged(event,ui)
{
	if(!ui.item[0]) return;
	var itemId = ui.item[0].id;
	var n = $(this).sortable('toArray');

	// remove possible empty id's
	for(var i=0; i<sortOrder.length; i++) {
		if(sortOrder[i] == '') { sortOrder.splice(i,1); i--; }
	}
	if(n.toString() == sortOrder.toString()) return;

	// make index: id=>position
	var h0 = {}; //before
	for(var j=0; j<sortOrder.length; j++) {
		h0[sortOrder[j]] = j;
	}
	var h1 = {}; //after
	for(var j=0; j<n.length; j++) {
		h1[n[j]] = j;
		taskOrder[j] = parseInt(n[j].split('_')[1]);
	}
    if (Object.keys(h0).length != Object.keys(h1).length) return; // move to another list
	// prepare param
	var o = [];
	var diff;
	var replaceOW = h1[itemId] ? taskList[sortOrder[h1[itemId]].split('_')[1]].orderIndex : null;
	for(var j in h0) {
        diff = h1[j] - h0[j];
        if(diff != 0) {
            var a = j.split('_');
            if(j == itemId) diff = replaceOW - taskList[a[1]].orderIndex;
            o.push({id:a[1], diff:diff});
            taskList[a[1]].orderIndex += diff;
        }
	}

	_mtt.db.request('changeOrder', {order:o});
};


function mttMenu(container, options)
{
	var menu = this;
	this.container = document.getElementById(container);
	this.$container = $(this.container);
	this.menuOpen = false;
	this.options = options || {};
	this.submenu = [];
	this.curSubmenu = null;
	this.showTimer = null;
	this.ts = (new Date).getTime();
	this.container.mttmenu = this.ts;

	this.$container.find('li').click(function(){
		menu.onclick(this, menu);
		return false;
	})
	.each(function(){

		var submenu = 0;
		if($(this).is('.mtt-menu-indicator'))
		{
			submenu = new mttMenu($(this).attr('submenu'));
			submenu.$caller = $(this);
			submenu.parent = menu;
			if(menu.root) submenu.root = menu.root;	//!! be careful with circular references
			else submenu.root = menu;
			menu.submenu.push(submenu);
			submenu.ts = submenu.container.mttmenu = submenu.root.ts;

			submenu.$container.find('li').click(function(){
				submenu.root.onclick(this, submenu);
				return false;
			});
		}

		$(this).hover(
			function(){
				if(!$(this).is('.mtt-menu-item-active')) menu.$container.find('li').removeClass('mtt-menu-item-active');
				clearTimeout(menu.showTimer);
				if(menu.hideTimer && menu.parent) {
					clearTimeout(menu.hideTimer);
					menu.hideTimer = null;
					menu.$caller.addClass('mtt-menu-item-active');
					clearTimeout(menu.parent.showTimer);
				}

				if(menu.curSubmenu && menu.curSubmenu.menuOpen && menu.curSubmenu != submenu && !menu.curSubmenu.hideTimer)
				{
					menu.$container.find('li').removeClass('mtt-menu-item-active');
					var curSubmenu = menu.curSubmenu;
					curSubmenu.hideTimer = setTimeout(function(){
						curSubmenu.hide();
						curSubmenu.hideTimer = null;
					}, 300);
				}

				if(!submenu || menu.curSubmenu == submenu && menu.curSubmenu.menuOpen)
					return;
			
				menu.showTimer = setTimeout(function(){
					menu.curSubmenu = submenu;
					submenu.showSub();
				}, 400);
			},
			function(){}
		);

	});

	this.onclick = function(item, fromMenu)
	{
		if($(item).is('.mtt-item-disabled,.mtt-menu-indicator,.mtt-item-hidden')) return;
		menu.close();
		if(this.options.onclick) this.options.onclick(item, fromMenu);
	};

	this.hide = function()
	{
		for(var i in this.submenu) this.submenu[i].hide();
		clearTimeout(this.showTimer);
		this.$container.hide();
		this.$container.find('li').removeClass('mtt-menu-item-active');
		this.menuOpen = false;
	};

	this.close = function(event)
	{
		if(!this.menuOpen) return;
		if(event)
		{
			// ignore if event (click) was on caller or container
			var t = event.target;
			if(t == this.caller || (t.mttmenu && t.mttmenu == this.ts)) return;
			while(t.parentNode) {
				if(t.parentNode == this.caller || (t.mttmenu && t.mttmenu == this.ts)) return;
				t = t.parentNode;
			}
		}
		this.hide();
		$(this.caller).removeClass('mtt-menu-button-active');
		$(document).unbind('mousedown.mttmenuclose');
	};

	this.show = function(caller)
	{
		if(this.menuOpen)
		{
			this.close();
			if(this.caller && this.caller == caller) return;
		}
		$(document).triggerHandler('mousedown.mttmenuclose'); //close any other open menu
		this.caller = caller;
		var $caller = $(caller);
		
		// beforeShow trigger
		if(this.options.beforeShow && this.options.beforeShow.call)
			this.options.beforeShow();

		// adjust width
		if(this.options.adjustWidth && this.$container.outerWidth(true) > $(window).width())
			this.$container.width($(window).width() - (this.$container.outerWidth(true) - this.$container.width()));

		$caller.addClass('mtt-menu-button-active');
		var offset = $caller.offset();
		var x2 = $(window).width() + $(document).scrollLeft() - this.$container.outerWidth(true) - 1;
		var x = offset.left < x2 ? offset.left : x2;
		if(x<0) x=0;
		var y = offset.top+caller.offsetHeight-1;
		if(y + this.$container.outerHeight(true) > $(window).height() + $(document).scrollTop()) y = offset.top - this.$container.outerHeight();
		if(y<0) y=0;
		this.$container.css({ position: 'absolute', top: y, left: x, width:this.$container.width() /*, 'min-width': $caller.width()*/ }).show();
		var menu = this;
		$(document).bind('mousedown.mttmenuclose', function(e){ menu.close(e) });
		this.menuOpen = true;
	};

	this.showSub = function()
	{
		this.$caller.addClass('mtt-menu-item-active');
		var offset = this.$caller.offset();
		var x = offset.left+this.$caller.outerWidth();
		if(x + this.$container.outerWidth(true) > $(window).width() + $(document).scrollLeft()) x = offset.left - this.$container.outerWidth() - 1;
		if(x<0) x=0;
		var y = offset.top + this.parent.$container.offset().top-this.parent.$container.find('li:first').offset().top;
		if(y +  this.$container.outerHeight(true) > $(window).height() + $(document).scrollTop()) y = $(window).height() + $(document).scrollTop()- this.$container.outerHeight(true) - 1;
		if(y<0) y=0;
		this.$container.css({ position: 'absolute', top: y, left: x, width:this.$container.width() /*, 'min-width': this.$caller.outerWidth()*/ }).show();
		this.menuOpen = true;
	};

	this.destroy = function()
	{
		for(var i in this.submenu) {
			this.submenu[i].destroy();
			delete this.submenu[i];
		}
		this.$container.find('li').unbind(); //'click mouseenter mouseleave'
	};
};


function taskContextMenu(el, id)
{
	if(!_mtt.menus.cmenu) _mtt.menus.cmenu = new mttMenu('taskcontextcontainer', {
		onclick: taskContextClick,
		beforeShow: function() {
			$('#cmenupriocontainer li').removeClass('mtt-item-checked');
			$('#cmenu_prio\\:'+ taskList[_mtt.menus.cmenu.tag].prio).addClass('mtt-item-checked');
		} 
	});
	_mtt.menus.cmenu.tag = id;
	_mtt.menus.cmenu.show(el);
};

function taskContextClick(el, menu)
{
	if(!el.id) return;
	var taskId = parseInt(_mtt.menus.cmenu.tag);
	var id = el.id, value;
	var a = id.split(':');
	if(a.length == 2) {
		id = a[0];
		value = a[1];
	}
	switch(id) {
		case 'cmenu_edit': editTask(taskId); break;
        case 'cmenu_clone': cloneTask(taskId); break;
		case 'cmenu_delete': _mtt.confirmAction('confirmDelete', 'deleteTask', taskId); break;
		case 'cmenu_prio': setTaskPrio(taskId, parseInt(value)); break;
		case 'cmenu_list':
			if(menu.$caller && menu.$caller.attr('id')=='cmenu_move') moveTaskToList(taskId, value);
			break;
	}
};


function moveTaskToList(taskId, listId)
{
	if(curList.id == listId) return;
	_mtt.db.request('moveTask', {id:taskId, from:curList.id, to:listId}, function(json){
		if(!parseInt(json.total)) return;
		if(curList.id == -1)
		{
			// leave the task in current tab (all tasks tab)
			var item = json.list[0];
			changeTaskCnt(item, 0, taskList[item.id]);
			taskList[item.id] = item;
			var noteExpanded = (item.note != '' && $('#taskrow_'+item.id).is('.task-expanded')) ? 1 : 0;
			$('#taskrow_'+item.id).replaceWith(prepareTaskStr(item, noteExpanded));
			if(curList.sort != "DEFAULT") changeTaskOrder(item.id);
			refreshTaskCnt();
			$('#taskrow_'+item.id).effect("highlight", {color:_mtt.theme.editTaskFlashColor}, 'normal', function(){$(this).css('display','')});
		}
		else {
			// remove the task from currrent tab
			changeTaskCnt(taskList[taskId], -1)
			delete taskList[taskId];
			taskOrder.splice($.inArray(taskId,taskOrder), 1);
			$('#taskrow_'+taskId).fadeOut('normal', function(){ $(this).remove() });
			refreshTaskCnt();
		}
	});

	$("#editTitle").flushCache();
	flag.tagsChanged = true;
};


function cmenuOnListsLoaded()
{
	if(_mtt.menus.cmenu) _mtt.menus.cmenu.destroy();
	_mtt.menus.cmenu = null;
	var s = '';
	var all = tabLists.getAll();
	for(var i in all) {
		s += '<li id="cmenu_list:'+all[i].id+'" class="'+(all[i].hidden?'mtt-list-hidden':'')+'">'+all[i].name+'</li>';
	}
	$('#cmenulistscontainer ul').html(s);
};

function cmenuOnListAdded(list)
{
	if(_mtt.menus.cmenu) _mtt.menus.cmenu.destroy();
	_mtt.menus.cmenu = null;
	$('#cmenulistscontainer ul').append('<li id="cmenu_list:'+list.id+'">'+list.name+'</li>');
};

function cmenuOnListRenamed(list)
{
	$('#cmenu_list\\:'+list.id).text(list.name);
};

function cmenuOnListSelected(list)
{
	$('#cmenulistscontainer li').removeClass('mtt-item-disabled');
	$('#cmenu_list\\:'+list.id).addClass('mtt-item-disabled').removeClass('mtt-list-hidden');
};

function cmenuOnListOrderChanged()
{
	cmenuOnListsLoaded();
	$('#cmenu_list\\:'+curList.id).addClass('mtt-item-disabled');
};

function cmenuOnListHidden(list)
{
	$('#cmenu_list\\:'+list.id).addClass('mtt-list-hidden');
};


function tabmenuOnListSelected(list)
{
	if(list.published) {
		$('#btnPublish').addClass('mtt-item-checked');
		$('#btnRssFeed').removeClass('mtt-item-disabled');
	}
	else {
		$('#btnPublish').removeClass('mtt-item-checked');
		$('#btnRssFeed').addClass('mtt-item-disabled');
	}
    if(list.watchedByMe) $('#btnWatch').addClass('mtt-item-checked');
    else $('#btnWatch').removeClass('mtt-item-checked');
	if(list.showCompleted) $('#btnShowCompleted').addClass('mtt-item-checked');
	else $('#btnShowCompleted').removeClass('mtt-item-checked');
    if (list.notesExpanded) $('#btnExpandNotes').addClass('mtt-item-checked');
    else $('#btnExpandNotes').removeClass('mtt-item-checked');
};


function listOrderChanged(event, ui)
{
	var a = $(this).sortable("toArray");
	var order = [];
	for(var i in a) {
		order.push(a[i].split('_')[1]);
	}
	tabLists.reorder(order);
	_mtt.db.request('changeListOrder', {order:order});
	_mtt.doAction('listOrderChanged', {order:order});
};

function showCompletedToggle()
{
	var act = curList.showCompleted ? 0 : 1;
	curList.showCompleted = tabLists.get(curList.id).showCompleted = act;
	if(act) $('#btnShowCompleted').addClass('mtt-item-checked');
	else $('#btnShowCompleted').removeClass('mtt-item-checked');
	loadTasks({setCompl:1});
};

function clearCompleted()
{
	if(!curList) return false;
	_mtt.db.request('clearCompletedInList', {list:curList.id}, function(json){
		if(!parseInt(json.total)) return;
		flag.tagsChanged = true;
		if(curList.showCompleted) loadTasks();
	});
};

function tasklistClick(e)
{
	var node = e.target.nodeName.toUpperCase();
	if(node=='SPAN' || node=='LI' || node=='DIV')
	{
		var li =  findParentNode(e.target, 'LI');
		if(li) {
			if(selTask && li.id != selTask) $('#'+selTask).removeClass('clicked doubleclicked');
			selTask = li.id;
			if($(li).is('.clicked')) $(li).toggleClass('doubleclicked');
			else $(li).addClass('clicked');
		}
	}
};


function showhide(a,b)
{
	a.show();
	b.hide();
};

function findParentNode(el, node)
{
	// in html nodename is in uppercase, in xhtml nodename in in lowercase
	if(el.nodeName.toUpperCase() == node) return el;
	if(!el.parentNode) return null;
	while(el.parentNode) {
		el = el.parentNode;
		if(el.nodeName.toUpperCase() == node) return el;
	}
};

function getLiTaskId(el)
{
	var li = findParentNode(el, 'LI');
	if(!li || !li.id) return 0;
	return li.id.split('_',2)[1];
};

function isParentId(el, id)
{
	if(el.id && $.inArray(el.id, id) != -1) return true;
	if(!el.parentNode) return null;
	return isParentId(el.parentNode, id);
};

function dehtml(str)
{
	return str.replace(/&quot;/g,'"').replace(/&lt;/g,'<').replace(/&gt;/g,'>').replace(/&amp;/g,'&');
};


function slmenuOnListsLoaded()
{
	if(_mtt.menus.selectlist) {
		_mtt.menus.selectlist.destroy();
		_mtt.menus.selectlist = null;
	}

	var s = '';
	var all = tabLists.getAll();
	for(var i in all) {
		s += '<li id="slmenu_list:'+all[i].id+'" class="'+(all[i].id==curList.id?'mtt-item-checked':'')+' list-id-'+all[i].id+(all[i].hidden?' mtt-list-hidden':'')+'"><div class="menu-icon"></div><a href="#list/'+all[i].id+'">'+all[i].name+'</a></li>';
	}
	$('#slmenucontainer ul>.slmenu-lists-begin').nextAll().remove();
	$('#slmenucontainer ul>.slmenu-lists-begin').after(s);
};

function slmenuOnListRenamed(list)
{
	$('#slmenucontainer li.list-id-'+list.id).find('a').html(list.name);
};

function slmenuOnListAdded(list)
{
	if(_mtt.menus.selectlist) {
		_mtt.menus.selectlist.destroy();
		_mtt.menus.selectlist = null;
	}
	$('#slmenucontainer ul').append('<li id="slmenu_list:'+list.id+'" class="list-id-'+list.id+'"><div class="menu-icon"></div><a href="#list/'+list.id+'">'+list.name+'</a></li>');
};

function slmenuOnListSelected(list)
{
	$('#slmenucontainer li').removeClass('mtt-item-checked');
	$('#slmenucontainer li.list-id-'+list.id).addClass('mtt-item-checked').removeClass('mtt-list-hidden');

};

function slmenuOnListHidden(list)
{
	$('#slmenucontainer li.list-id-'+list.id).addClass('mtt-list-hidden');
};

function slmenuSelect(el, menu)
{
	if(!el.id) return;
	var id = el.id, value;
	var a = id.split(':');
	if(a.length == 2) {
		id = a[0];
		value = a[1];
	}
	if(id == 'slmenu_list') {
		tabSelect(parseInt(value));
	}
	return false;
};


function exportCurList(format)
{
	if(!curList) return;
	if(!format.match(/^[a-z0-9-]+$/i)) return;
	window.location.href = _mtt.mttUrl + 'export.php?list='+curList.id +'&format='+format;
};

function feedCurList()
{
	if(!curList) return;
	window.location.href = _mtt.mttUrl + 'feed.php?list='+curList.id;
}

function hideTab(listId)
{
	if(typeof listId != 'number') {
		var id = $(listId).attr('id');
		if(!id) return;
		listId = parseInt(id.split('_', 2)[1]);
	}
	
	if(!tabLists.get(listId)) return false;

	// if we hide current tab
	var listIdToSelect = 0;
	if(curList.id == listId) {
		var all = tabLists.getAll();
		for(var i in all) {
			if(all[i].id != curList.id && !all[i].hidden) {
				listIdToSelect = all[i].id;
				break;
			}
		}
		// do not hide the tab if others are hidden
		if(!listIdToSelect) return false;
	}

	if(listId == -1) {
		$('#list_all').addClass('mtt-tabs-hidden').removeClass('mtt-tabs-selected');
	}
	else {
		$('#list_'+listId).addClass('mtt-tabs-hidden').removeClass('mtt-tabs-selected');
	}
	
	tabLists.get(listId).hidden = true;
	
	if(listId > 0) {
		_mtt.db.request('setHideList', {list:listId, hide:1});
		_mtt.doAction('listHidden', tabLists.get(listId));
	}
	
	if(listIdToSelect) {
		tabSelect(listIdToSelect);
	}
}

function checkAllListsTab() {
    if (tabLists.length()>2) {
        $("#list_all").removeClass('mtt-tabs-hidden');
    } else {
        $('#list_all').addClass('mtt-tabs-hidden');
    }
}

function getNoteFrameDocument() {
    var noteFrame = document.getElementById("noteFrame");
    if (noteFrame) {
        return noteFrame.contentWindow.document;
    } else {
        return null;
    }
}
function writeToNoteFrame(notes) {
    var d = getNoteFrameDocument();
    if (d) {
        d.open();
        d.close();
        $("body", d).attr('style','font-family:arial;font-size:12px;color:#444');
        $("body", d).html(notes);
    }
}

function getDate(date) {
    if (!date) return new Date(0);
    var dayHour = date.split(" ");
    var day = dayHour[0].split("/");
    var hour = dayHour.length > 1 ? dayHour[1].split(":") : null;
    if (hour) {
        return new Date(day[2], day[0]-1, day[1], hour[0], hour[1], hour[2]);
    } else {
        return new Date(day[2], day[0]-1, day[1]);
    }
}

function smartDate(date, now) {
    if (!date) return '';
    if (now) {
        var dateInDays = (getDate(date).getTime() - getDate(now).getTime()) / 86400000;
        if (-dateInDays * 86400 < 6) return _mtt.lang.get('time_amomentago');
        else if (-dateInDays * 86400 < 30) return _mtt.lang.get('time_secondsago').replace('%s', Math.round(-dateInDays*86400));
        else if (-dateInDays * 1440 < 1.5) return _mtt.lang.get('time_oneminuteago');
        else if (-dateInDays * 1440 < 60) return _mtt.lang.get('time_minutesago').replace('%s', Math.round(-dateInDays*1440));
        else if (-dateInDays * 24 < 1.5) return _mtt.lang.get('time_onehourago');
        else if (-dateInDays * 24 < 24) return _mtt.lang.get('time_hoursago').replace('%s', Math.round(-dateInDays*24));
        else if (-dateInDays < 1.5) return _mtt.lang.get('time_onedayago');
        else if (-dateInDays < 7) return _mtt.lang.get('time_daysago').replace('%s', Math.round(-dateInDays));
        else {
            return date.split(" ")[0];
        };
    } else {
        var dayHour = date.split(" ");
        return dayHour[0];
    }

}
/*
	Errors and Info messages
*/

function flashError(str, details)
{
	$("#msg>.msg-text").text(str)
	$("#msg>.msg-details").text(details);
	$("#loading").hide();
	$("#msg").addClass('mtt-error').effect("highlight", {color:_mtt.theme.msgFlashColor}, 700);
}

function flashInfo(str, details)
{
	$("#msg>.msg-text").text(str)
	$("#msg>.msg-details").text(details);
	$("#loading").hide();
	$("#msg").addClass('mtt-info').effect("highlight", {color:_mtt.theme.msgFlashColor}, 700);
}

function toggleMsgDetails()
{
	var el = $("#msg>.msg-details");
	if(!el) return;
	if(el.css('display') == 'none') el.show();
	else el.hide()
}


/*
	Authorization
*/
function updateAccessStatus()
{
	// flag.needAuth is not changed after pageload
	if(flag.needAuth)
	{
		$('#bar_auth').show();
		if(flag.isLogged) {
			showhide($("#bar_logout"),$("#bar_login"));
			$('#bar .menu-owner').show();
			$('#bar .bar-delim').show();
		}
		else {
			showhide($("#bar_login"),$("#bar_logout"));
			$('#bar .menu-owner').hide();
			$('#bar .bar-delim').hide();
		}
	}
	else {
		$('#bar .menu-owner').show();
	}
	if(flag.needAuth && !flag.isLogged) {
		flag.readOnly = true;
		$("#bar_public").show();
		$('#mtt_body').addClass('readonly')
		liveSearchToggle(1);
		// remove some tab menu items
		$('#btnRenameList,#btnDeleteList,#btnClearCompleted,#btnPublish').remove();
	}
	else {
		flag.readOnly = false;
		$('#mtt_body').removeClass('readonly')
		$("#bar_public").hide();
		liveSearchToggle(0);
	}
	$('#page_ajax').hide();
}

function showAuth(el)
{
	var w = $('#authform');
	if(w.css('display') == 'none')
	{
		var offset = $(el).offset();
		w.css({
			position: 'absolute',
			top: offset.top + el.offsetHeight + 3,
			left: offset.left + el.offsetWidth - w.outerWidth()
		}).show();
		$('#password').focus();
	}
	else {
		w.hide();
		el.blur();
	}
}

function doAuth(form)
{
	$.post(mytinytodo.mttUrl+'ajax.php?login', { login:1, password: form.password.value }, function(json){
		form.password.value = '';
		if(json.logged)
		{
			flag.isLogged = true;
			window.location.reload();
		}
		else {
			flashError(_mtt.lang.get('invalidpass'));
			$('#password').focus();
		}
	}, 'json');
	$('#authform').hide();
}

function logout()
{
	$.post(mytinytodo.mttUrl+'ajax.php?logout', { logout:1 }, function(json){
		flag.isLogged = false;
		window.location.reload();
	}, 'json');
	return false;
} 

/*
	Settings
*/

function showSettings()
{
	$('#page_ajax').load(_mtt.mttUrl+'Projects/edit?projectId=' + _mtt.project,null,function(){
		_mtt.pageSet('ajax','settings');
        $('#settings').hide();
	})
	return false;
}

function saveSettings(frm)
{
	if(!frm) return false;
	var params = { save:'ajax' };
    params["projectId"] = _mtt.project;
	$(frm).find("input:text,input:password,input:checked,select").filter(":enabled").each(function() { params[this.name || '__'] = this.value; }); 
	$(frm).find(":submit").attr('disabled','disabled').blur();
	$.post(_mtt.mttUrl+'Projects/save', params, function(json){
		if(json.saved) {
			flashInfo(_mtt.lang.get('settingsSaved'));
			setTimeout('window.location.reload();', 1000);
		}
	}, 'json');
}

})();