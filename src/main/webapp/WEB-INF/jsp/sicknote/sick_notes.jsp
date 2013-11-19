
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@taglib prefix="joda" uri="http://www.joda.org/joda/time/tags" %>
<%@taglib prefix="form" uri="http://www.springframework.org/tags/form" %>


<!DOCTYPE html>
<html>

<head>
    <title><spring:message code="title" /></title>
    <%@include file="../include/header.jsp" %>
</head>

<body>

<spring:url var="formUrlPrefix" value="/web" />

<%@include file="../include/menu_header.jsp" %>

<div id="content">
    <div class="container_12">

        <div class="grid_12">

            <div class="overview-header">

                <legend>
                    <p>
                        <spring:message code="sicknotes" />
                    </p>
                    <div class="btn-group sicknote-button">

                        <a class="btn" href="${formUrlPrefix}/sicknote/new">
                            <i class="icon-plus"></i>&nbsp;<spring:message code="sicknotes.new" />
                        </a>

                    </div>
                </legend>

            </div>

            <c:choose>

                <c:when test="${empty sickNotes}">
                    <spring:message code="sicknotes.none" />
                </c:when>

                <c:otherwise>
                    <table class="app-tbl sortable-tbl tablesorter zebra-table" cellspacing="0">
                        <thead>
                        <tr>
                            <th><spring:message code="name" /></th>
                            <th><spring:message code="time" /></th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach items="${sickNotes}" var="sickNote" varStatus="loopStatus">
                            <tr>
                                <td>
                                    <c:out value="${sickNote.person.firstName}" />&nbsp;<c:out value="${sickNote.person.lastName}" />
                                </td>
                                <td>
                                    <joda:format style="M-" value="${sickNote.startDate}"/>&nbsp;-&nbsp;<joda:format style="M-" value="${sickNote.endDate}"/>
                                </td>
                        </c:forEach>
                        </tbody>
                    </table>
                </c:otherwise>

            </c:choose>

        </div>
    </div>
</div>

</body>

</html>
