<%@taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<%@attribute name="id" type="java.lang.String" required="true" %>
<%@attribute name="actionUrl" type="java.lang.String" required="true" %>

<div id="${id}" class="modal fade" tabindex="-1" role="dialog" aria-labelledby="filterModalLabel"
     aria-hidden="true">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal" aria-hidden="true"><i class="fa fa-remove"></i></button>
                <h4 id="filterModalLabel" class="modal-title"><spring:message code="filter.title"/></h4>
            </div>
            <form:form method="POST" action="${actionUrl}" modelAttribute="filterRequest" class="form-horizontal">
                <div class="modal-body">
                    <div class="form-group">
                        <label class="control-label col-sm-4">
                            <spring:message code="filter.period"/>
                        </label>
                        <div class="col-sm-7 radio">
                            <label class="thirds">
                                <form:radiobutton id="periodYear" path="period" value="YEAR"/>
                                <spring:message code="filter.period.year"/>
                            </label>
                            <label class="thirds">
                                <form:radiobutton id="periodQuarter" path="period" value="QUARTER"/>
                                <spring:message code="filter.period.quarter"/>
                            </label>
                            <label class="thirds">
                                <form:radiobutton id="periodMonth" path="period" value="MONTH"/>
                                <spring:message code="filter.period.month"/>
                            </label>
                        </div>
                    </div>
                </div>
                <div class="modal-footer">
                    <button class="btn btn-primary is-sticky" type="submit">
                        <spring:message code="action.confirm"/>
                    </button>
                </div>
            </form:form>
        </div>
    </div>
</div>