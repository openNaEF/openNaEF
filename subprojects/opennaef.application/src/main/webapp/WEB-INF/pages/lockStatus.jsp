<html>
<head>
    <title>DiffStatus</title>
    <%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

    <script language="JavaScript">
        <!--
        function createHttpRequest() {
            if (window.ActiveXObject) {
                try {
                    return new ActiveXObject("Msxml2.XMLHTTP")
                } catch (e) {
                    try {
                        return new ActiveXObject("Microsoft.XMLHTTP")
                    } catch (e2) {
                        return null
                    }
                }
            } else if (window.XMLHttpRequest) {
                return new XMLHttpRequest()
            } else {
                return null
            }
        }

        function startRequest(category) {
            var url = "./diff?cmd=createDiff&category=" + category
            httpRequest(url)
        }

        function interruptRequest(category) {
            var url = "./diff?cmd=createDiffInterrupt&category=" + category
            httpRequest(url)
        }

        function unlock(category) {
            var url = "./diff?cmd=unlockForce&category=" + category
            httpRequest(url)
        }

        function httpRequest(url) {
            var http = createHttpRequest()
            http.open("GET", url)
            http.onreadystatechange = function () {
                if (http.readyState == 4) onLoaded(http)
            }
            http.send(null)
        }

        function onLoaded(http) {
            if (http.status != 200) {
                alert(http.responseText)
            }
            location.reload()
        }
        //-->
    </script>

    <link href="../diff.css" rel="stylesheet" type="text/css"/>
</head>
<body>

<h1>差分ステータス</h1>
<br>
<a href="#" onclick="location.reload()">[refresh]</a>
<br>
<br>
<table class="data_table">
    <tr>
        <th class="title_cell">対象</th>
        <th class="title_cell">状態</th>
        <th class="title_cell">操作</th>
        <th class="title_cell">直前の実行結果</th>
        <th class="title_cell">時間</th>
        <th class="title_cell">ロックしているユーザ名</th>
    </tr>
    <tr>
        <td class="cell_border_all">Discovery</td>
        <td class="cell_border_all">
            <c:choose>
                <c:when test="${!isDiscoveryRunning}">Idle</c:when>
                <c:when test="${isDiscoveryRunning}">Running</c:when>
            </c:choose>
        </td>
        <td class="cell_border_all">
            <c:choose>
                <c:when test="${!isDiscoveryRunning}"><input type="button" value="start diff generation"
                                                             onclick="startRequest('DISCOVERY')"/></c:when>
                <c:when test="${isDiscoveryRunning}"><input type="button" value="abort diff generation"
                                                            onclick="interruptRequest('DISCOVERY')"/></c:when>
            </c:choose>
        </td>
        <td class="cell_border_all">${discoveryLatestResult}</td>
        <td class="cell_border_all">${discoveryLatestDate}</td>
        <td class="cell_border_all">
            ${discoveryLockUser} <c:if test="${discoveryLockUser!=''&&discoveryLockUser!='System'}"><input type="button"
                                                                                                           value="release lock"
                                                                                                           onclick="unlock('DISCOVERY')"/></c:if>
        </td>
    </tr>
</table>


</body>
</html>