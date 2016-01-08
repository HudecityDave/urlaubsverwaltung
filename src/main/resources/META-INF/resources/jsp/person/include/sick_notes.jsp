<%@page contentType="text/html" pageEncoding="UTF-8" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@taglib prefix="joda" uri="http://www.joda.org/joda/time/tags" %>
<%@taglib prefix="uv" tagdir="/WEB-INF/tags" %>

<table class="list-table bordered-table selectable-table" cellspacing="0">
    <tbody>
    <c:forEach items="${sickNotes}" var="sickNote" varStatus="loopStatus">
    <c:choose>
        <c:when test="${sickNote.active}">
            <c:set var="CSS_CLASS" value="active"/>
        </c:when>
        <c:otherwise>
            <c:set var="CSS_CLASS" value="inactive"/>
        </c:otherwise>
    </c:choose>
    <tr class="${CSS_CLASS}" onclick="navigate('${URL_PREFIX}/sicknote/${sickNote.id}');">
        <td class="is-centered state ${sickNote.type}">
            <span class="hidden-print">
                <c:choose>
                    <c:when test="${sickNote.type == 'SICK_NOTE_CHILD'}">
                        <i class="fa fa-child"></i>
                    </c:when>
                    <c:otherwise>
                        <i class="fa fa-medkit"></i>
                    </c:otherwise>
                </c:choose>
            </span>
        </td>
        <td>
            <a href="${URL_PREFIX}/sicknote/${sickNote.id}" class="hidden-print">
                <h4>
                    <spring:message code="sicknote.title" />
                </h4>
            </a>

            <h4 class="visible-print">
                <spring:message code="sicknote.title" />
            </h4>

            <p>
                <uv:date date="${sickNote.startDate}"/> - <uv:date date="${sickNote.endDate}"/>

                <c:if test="${sickNote.aubPresent == true}">
                    <span class="visible-xs-block visible-sm-inline-block visible-md-inline-block visible-lg-inline-block">
                       (<i class="fa fa-check positive"></i> <spring:message code="sicknote.data.aub.short" />)
                    </span>
                </c:if>
            </p>
        </td>
        <td class="is-centered hidden-xs">
            <span><uv:number number="${sickNote.workDays}"/> <spring:message code="duration.days" /></span>
        </td>
        <td class="hidden-print is-centered hidden-xs">
            <i class="fa fa-clock-o"></i> <span><spring:message code="sicknote.progress.lastEdited" /> <uv:date date="${sickNote.lastEdited}"/></span>
        </td>
        </c:forEach>
    </tbody>
</table>
