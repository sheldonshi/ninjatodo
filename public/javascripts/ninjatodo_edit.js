/*
 (C) Copyright 2012 Sheldon Shi
 Licensed under the GNU GPL v3 license. See file COPYRIGHT for details.
 */

(function(){
    if (_mtt.editJSLoaded) {
        return;
    }
    _mtt.editJSLoaded = true;

    $(".inviteToProject a").live("click", function() {
        var invite = $(this).parent().next()[0];
        if ($(invite).css('display') == 'none') {
            _mtt.db.request('invitations', {project:_mtt.project}, function(json){
                showInvitations('addToProject_READ', json);
                showInvitations('addToProject_WRITE', json);
                showInvitations('addToProject_OWN', json);
            });
            $(invite).slideDown('fast');
        }
        else $(invite).slideUp('fast');
        return false;
    })

    $('.addToProject .ntt-action-invite').live('click', function(){
        var textareaVal = $(this).parent().parent().find('textarea').val();
        var elemId = $(this).parent().parent()[0].id;
        var role = elemId.split('_')[1];
        if (textareaVal != '') {
            _mtt.db.request('invite', {project:_mtt.project, emails:textareaVal, role:role}, function(json){
                showInvitations(elemId, json);
                $('#'+elemId+' textarea').val('');
            });
        }
        return false;
    });

    $('.addToProject .ntt-action-cancel').live('click', function(){
        $(this).parent().parent().find('textarea').val('');
        $(this).parent().parent()[0].slideUp('fast');
        return false;
    });

    $('.pendingMembers a.deleteInvitedEmail').live('click', function(){
        var id=$(this).parent()[0].id.split('_')[1];
        _mtt.confirmAction("deleteInvitation", "deleteInvitation", id);
        return false;
    });

    $('span .team-members').hover(
        function() {
            if ($(this).parents('#ownerList').length > 0 && $('#ownerList').find(".team-members").length<=2) {
                return; // only owner
            } else {
                $(this).find('.team-member-action').show();
            }
        },
        function() {
            $(this).find('.team-member-action').hide();
        }
    );

    $('a.deleteMember').live('click', function(){
        var id=$(this)[0].id.split('_')[1];
        _mtt.confirmAction("deleteMember", "deleteMember", id);
        return false;
    });

    $('a.promoteMember').live('click', function(){
        var id=$(this)[0].id.split('_')[1];
        _mtt.db.request('promoteMember', {id:id, project:_mtt.project}, function(json){
            var newRoleArea = '';
            if ($('#participant_'+id).parents('#adminList').length > 0) newRoleArea = '#ownerList';
            else if ($('#participant_'+id).parents('#memberList').length > 0) newRoleArea = '#adminList';
            if (newRoleArea != '') {
                $('#participant_'+id).parents('.eachMember').prependTo(newRoleArea);
                $('#participant_'+id).parents('.eachMember').find('.team-member-action').hide();
            }
        });
        return false;
    });

    $('a.demoteMember').live('click', function(){
        var id=$(this)[0].id.split('_')[1];
        _mtt.db.request('demoteMember', {id:id, project:_mtt.project}, function(json){
            var newRoleArea = '';
            if ($('#participant_'+id).parents('#adminList').length > 0) newRoleArea = '#memberList';
            else if ($('#participant_'+id).parents('#ownerList').length > 0) newRoleArea = '#adminList';
            if (newRoleArea != '') {
                $('#participant_'+id).parents('.eachMember').prependTo(newRoleArea);
                $('#participant_'+id).parents('.eachMember').find('.team-member-action').hide();
            }
        });
        return false;
    });

    function showInvitations(elemId, json) {
        if (!parseInt(json.total)) return;
        var str = '';
        $.each(json.list, function (i, item) {
            if (elemId=='addToProject_' + item.role) {
                str += "<div id='invited_"+item.id+"' class='invitedEmail'>" + item.toEmail + " <a href='#' class='deleteInvitedEmail weak'>X</a></div>";
            }
        });
        if (str != '') {
            if ($('#'+elemId).find('.pendingMembers') == null || $('#'+elemId).find('.pendingMembers').length == 0) {
                $('#'+elemId).append("<div class='pendingMembers'><strong>"+_mtt.lang.get('set_pendingMembers')+"</strong></div>");
            } else {
                $('#'+elemId).find('.pendingMembers').html('<strong>'+_mtt.lang.get('set_pendingMembers')+'</strong>');
            }
            $('#'+elemId).find('.pendingMembers').append(str);
        }
    };

    _mtt.deleteInvitation = function(id) {
        _mtt.db.request('deleteInvitation', {id:id, project:_mtt.project}, function(json){
            $('#invited_'+json.id).remove();
        });
    };

    _mtt.deleteMember = function(id) {
        _mtt.db.request('deleteMember', {id:id, project:_mtt.project}, function(text){
            if (text.length>0) $('#deleteMember_'+text).parent().parent().parent().remove();
        });
    };

    _mtt.promoteMember = function(id) {
        _mtt.db.request('promoteMember', {id:id, project:_mtt.project}, function(text){
            if (text.length>0) $('#deleteMember_'+text).parent().parent().parent().remove();
        });
    };

    _mtt.demoteMember = function(id) {
        _mtt.db.request('demoteMember', {id:id, project:_mtt.project}, function(text){
            if (text.length>0) $('#deleteMember_'+text).parent().parent().parent().remove();
        });
    };

})();