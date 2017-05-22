document.addEventListener("DOMContentLoaded", function () {
    fetchHooks();

    // init dialog
    var dialog = document.querySelector('dialog');
    dialog.querySelector('.close').addEventListener('click', function () {
        dialog.close();
    });

    // init text-fields
    (function () {
        var tfs = document.querySelectorAll(
            '.mdc-textfield:not([data-demo-no-auto-js])'
        );
        for (var i = 0, tf; tf = tfs[i]; i++) {
            mdc.textfield.MDCTextfield.attachTo(tf);
        }
    })();

    // init icons
    document.getElementById("refresh").addEventListener('click', fetchHooks, false);
    document.getElementById("addHook").addEventListener('click', editHook.bind({
        callback_url: "",
        active: true
    }), false);

    // init submit
    document.getElementById("updateHook").addEventListener('click', handleHookSubmit, false);
}, false);

function fetchHooks() {
    var xhr = new XMLHttpRequest();
    xhr.open('GET', location.origin + '/opennaef.notification/hooks', true);

    xhr.onreadystatechange = function (e) {
        if (this.readyState == 4 && this.status == 200) {
            var fragment = document.createDocumentFragment();
            var hooks = JSON.parse(this.responseText);
            for (var i = 0; i < hooks.length; i++) {
                var hook = hooks[i];
                // TODO table にするべき
                var li = document.createElement('li');
                li.className = 'mdc-list-item';

                var avator = document.createElement('i');
                avator.setAttribute('aria-hidden', true);
                var avatorClass = 'material-icons mdc-list-item__start-detail';
                var avatorType = 'notifications_active';
                if (hook.failed) {
                    avatorType = 'report';
                    avatorClass += ' material-icons__alert'
                } else if (!hook.active) {
                    avatorType = 'notifications';
                    avatorClass += ' gray'
                }
                avator.className = avatorClass;
                avator.appendChild(document.createTextNode(avatorType));
                li.appendChild(avator);

                li.appendChild(document.createTextNode(hook.callback_url));

                // var pingIcon = document.createElement('a');
                // pingIcon.className = 'mdc-list-item__end-detail material-icons';
                // pingIcon.setAttribute('aria-label', 'ping');
                // pingIcon.setAttribute('title', 'ping');
                // pingIcon.appendChild(document.createTextNode('notification_active'));
                // li.appendChild(pingIcon);

                var editIcon = document.createElement('a');
                editIcon.className = 'mdc-list-item__end-detail material-icons';
                editIcon.setAttribute('aria-label', 'edit');
                editIcon.setAttribute('title', 'edit');
                editIcon.appendChild(document.createTextNode('edit'));
                editIcon.addEventListener('click', editHook.bind(hook), false);
                li.appendChild(editIcon);

                fragment.appendChild(li)
            }
            var parent = document.getElementById('hook-list');
            parent.appendChild(fragment);
        }
    };
    clear(document.getElementById('hook-list'));
    xhr.send();
}

function editHook() {
    var editModal = document.getElementById('editHook');
    editModal.showModal();

    var hook = this;
    document.fHook['h-id'].value = hook.id;
    document.fHook['tf-callback_url'].value = hook.callback_url;
    document.fHook['cb-active'].checked = hook.active;
}

function handleHookSubmit() {
    var id = document.fHook['h-id'].value;      // undefined の場合は新規作成する
    var callbackURL = document.fHook['tf-callback_url'].value;
    var active = document.fHook['cb-active'].checked;

    var httpMethod = id === 'undefined' ? 'POST' : 'PATCH';
    var uri = id === 'undefined'
        ? location.origin + '/opennaef.notification/hooks'
        : location.origin + '/opennaef.notification/hooks/' + id;

    var xhr = new XMLHttpRequest();
    xhr.open(httpMethod, uri, false);
    xhr.setRequestHeader('Content-Type', 'application/json');
    xhr.onreadystatechange = function (e) {
        if (this.readyState == 4 && this.status == 200) {
            console.log(this);
        }
    };

    var reqBody = {
        id: id,
        callback_url: callbackURL,
        active: active
    };
    xhr.send(JSON.stringify(reqBody));
}

function clear(element) {
    while (element.firstChild) element.removeChild(element.firstChild);
}