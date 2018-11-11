<%@ page contentType="text/html;charset=UTF-8" %>
<%@ include file="/WEB-INF/views/include/taglib.jsp" %><%--
<html>
<head>
	<title>菜单导航</title>
	<meta name="decorator" content="blank"/>
	<script type="text/javascript">
		$(document).ready(function() {
			$(".accordion-heading a").click(function(){
				$('.accordion-toggle i').removeClass('icon-chevron-down');
				$('.accordion-toggle i').addClass('icon-chevron-right');
				if(!$($(this).attr('href')).hasClass('in')){
					$(this).children('i').removeClass('icon-chevron-right');
					$(this).children('i').addClass('icon-chevron-down');
				}
			});
			$(".accordion-body a").click(function(){
				$("#menu-${param.parentId} li").removeClass("active");
				$("#menu-${param.parentId} li i").removeClass("icon-white");
				$(this).parent().addClass("active");
				$(this).children("i").addClass("icon-white");
				//loading('正在执行，请稍等...');
			});
			//$(".accordion-body a:first i").click();
			//$(".accordion-body li:first li:first a:first i").click();
		});
	</script>
</head>
<body> --%>       <%--accordion的id，对标相应的 ↓ 一级菜单(一级菜单 指 首部的横向navbar中的菜单部分)的id--%>
<div class="accordion" id="menu-${param.parentId}">
    <c:set var="menuList" value="${fns:getMenuList()}"/>
    <c:set var="firstMenu" value="true"/>
    <c:forEach items="${menuList}" var="menu" varStatus="idxStatus">    <%--二级菜单var menu--%>    <%--指定的varStatus "idxStatus"没用到...orz...--%>
        <c:if test="${menu.parent.id eq (not empty param.parentId ? param.parentId : 1) && menu.isShow eq '1'}">
            <div class="accordion-group">                                       <%-- ↑ 1 乃“功能菜单”，一级菜单的parentId，根--%>
                <div class="accordion-heading">
                    <a class="accordion-toggle" data-toggle="collapse" data-parent="#menu-${param.parentId}"
                       data-href="#collapse-${menu.id}" href="#collapse-${menu.id}" title="${menu.remarks}">
                        <i class="icon-chevron-${not empty firstMenu && firstMenu ? 'down' : 'right'}"></i>&nbsp;${menu.name} <%--默认展开首个二级菜单，icon也要保持一致，icon-chevron-down--%>
                    </a>
                </div>
                <div id="collapse-${menu.id}" class="accordion-body collapse ${not empty firstMenu && firstMenu ? 'in' : ''}">  <%--新bootstrap貌似不用 in 而用更直观的 show 了--%>
                    <div class="accordion-inner">
                        <ul class="nav nav-list">
                            <c:forEach items="${menuList}" var="menu2"> <%--三级菜单var menu2--%>
                            <c:if test="${menu2.parent.id eq menu.id && menu2.isShow eq '1'}">
                                <li>
                                    <a data-href=".menu3-${menu2.id}"
                                       href="${fn:indexOf(menu2.href, '://') eq -1 ? ctx : ''}${not empty menu2.href ? menu2.href : '/404'}"    <%--【扣】(三级菜单都有href)这a/404，强行试了下，是"页面不存在"，换成/200也是，应该是没有响应的request mapping的缘故...--%>
                                       target="${not empty menu2.target ? menu2.target : 'mainFrame'}">     <%--menu有target的情况，都是 _blank，所以...--%>
                                        <i class="icon-${not empty menu2.icon ? menu2.icon : 'circle-arrow-right'}"></i>&nbsp;${menu2.name}
                                    </a>
                                    <ul class="nav nav-list hide" style="margin:0;padding-right:0;"><%--【扣】hide的 四级菜单？--%>
                                        <c:forEach items="${menuList}" var="menu3">                 <%--四级菜单？ var menu3--%>
                                            <c:if test="${menu3.parent.id eq menu2.id&&menu3.isShow eq '1'}">   <%--【鸣】目前没有isShow的四级菜单--%>
                                                <li class="menu3-${menu2.id} hide">
                                                    <a href="${fn:indexOf(menu3.href, '://') eq -1 ? ctx : ''}${not empty menu3.href ? menu3.href : '/404'}"
                                                       target="${not empty menu3.target ? menu3.target : 'mainFrame'}">
                                                        <i class="icon-${not empty menu3.icon ? menu3.icon : 'circle-arrow-right'}"></i>&nbsp;${menu3.name}
                                                    </a>
                                                </li>
                                            </c:if>
                                        </c:forEach>
                                    </ul>
                                </li>
    <c:set var="firstMenu" value="false"/>
                            </c:if>
                            </c:forEach>
                        </ul>
                    </div>
                </div>
            </div>
        </c:if>
    </c:forEach>
</div><%--
</body>
</html> --%>