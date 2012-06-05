/* for editing project form of ninja to do
 */

(function(){
    $("#addMember").live("click", function() {
        if ($('#addMemberToProject').css('display') == 'none') {
            _mtt.db.request('invitations', {project:_mtt.project}, function(json){
                showInvitations(json);
            });
            $('#addMemberToProject').slideDown('fast');
        }
        else $('#addMemberToProject').slideUp('fast');
        return false;
    })

    $('#addMemberToProject .ntt-action-invite').live('click', function(){
        if ($('#addMemberToProject textarea').val() != '') {
            _mtt.db.request('invite', {project:_mtt.project, emails:$('#addMemberToProject textarea').first().val()}, function(json){
                showInvitations(json);
                $('#addMemberToProject textarea').val('');
            });
        }
        return false;
    });

    $('#addMemberToProject .ntt-action-cancel').live('click', function(){
        $('#addMemberToProject textarea').val('');
        $('#addMemberToProject').slideUp('fast');
        return false;
    });

    $("#addAdmin").live("click", function() {
        if ($('#addAdminToProject').css('display') == 'none') {
            $('#addAdminToProject').slideDown('fast');
        }
        else $('#addAdminToProject').slideUp('fast');
        return false;
    })

    $('#addAdminToProject .ntt-action-promote').live('click', function(){
        if ($('#addMemberToProject select').val() != '') {
            _mtt.db.request('promoteToAdmin', {project:_mtt.project, participations:$('#addAdminToProject select').first().val()}, function(text){
                $('#adminList').html(text);
            });
        }
        return false;
    });

    $('#addAdminToProject .ntt-action-cancel').live('click', function(){
        $('#addAdminToProject select').val('');
        $('#addAdminToProject').slideUp('fast');
        return false;
    });

    $('#pendingMembers a.deleteInvitedEmail').live('click', function(){
        var id=$(this).parent()[0].id.split('_')[1];
        _mtt.confirmAction("deleteInvitation", "deleteInvitation", id);
        return false;
    });

    $('#memberList .delete-team-member a').live('click', function(){
        var id=$(this)[0].id.split('_')[1];
        _mtt.confirmAction("deleteMember", "deleteMember", id);
        return false;
    });

    $('#adminList .delete-team-member a').live('click', function(){
        var id=$(this)[0].id.split('_')[1];
        _mtt.confirmAction("deleteAdmin", "deleteAdmin", id);
        return false;
    });

    function showInvitations(json) {
        if (!parseInt(json.total)) return;
        if ($('#pendingMembers') == null || $('#pendingMembers').length == 0) {
            $('#addMemberToProject').append("<div id='pendingMembers'><strong>"+_mtt.lang.get('set_pendingMembers')+"</strong></div>");
        } else {
            $('#pendingMembers').html('<strong>'+_mtt.lang.get('set_pendingMembers')+'</strong>');
        }
        $.each(json.list, function (i, item) {
            $('#pendingMembers').append("<div id='invited_"+item.id+"' class='invitedEmail'>" + item.toEmail + " " +"<a href='#' class='deleteInvitedEmail weak'>X</a></div>");
        });
    };

    _mtt.deleteInvitation = function(id) {
        _mtt.db.request('deleteInvitation', {id:id, project:_mtt.project}, function(json){
            $('#invited_'+json.id).remove();
        });
    };

    _mtt.deleteAdmin = function(id) {
        _mtt.db.request('deleteAdmin', {id:id, project:_mtt.project}, function(text){
            if (text.length>0) $('#deleteAdmin_'+text).parent().parent().remove();
        });
    };

    _mtt.deleteMember = function(id) {
        _mtt.db.request('deleteMember', {id:id, project:_mtt.project}, function(text){
            if (text.length>0) $('#deleteMember_'+text).parent().parent().remove();
        });
    };
})();