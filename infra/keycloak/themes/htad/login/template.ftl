<#macro registrationLayout bodyClass="" displayInfo=false displayMessage=true displayRequiredFields=false showAnotherWayIfPresent=true>
<!DOCTYPE html>
<html<#if realm.internationalizationEnabled> lang="${locale.currentLanguageTag}"</#if>>
<head>
    <meta charset="utf-8">
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="robots" content="noindex, nofollow">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${msg("loginTitle",(realm.displayName!''))}</title>
    <link rel="icon" href="${url.resourcesPath}/img/logo.png" />
    <#if properties.stylesCommon?has_content>
        <#list properties.stylesCommon?split(' ') as style>
            <link href="${url.resourcesCommonPath}/${style}" rel="stylesheet" />
        </#list>
    </#if>
    <#if properties.styles?has_content>
        <#list properties.styles?split(' ') as style>
            <link href="${url.resourcesPath}/${style}" rel="stylesheet" />
        </#list>
    </#if>
    <link href="${url.resourcesPath}/css/login.css" rel="stylesheet" />
    <#if properties.scripts?has_content>
        <#list properties.scripts?split(' ') as script>
            <script src="${url.resourcesPath}/${script}" type="text/javascript"></script>
        </#list>
    </#if>
    <#if scripts??>
        <#list scripts as script>
            <script src="${script}" type="text/javascript"></script>
        </#list>
    </#if>
</head>
<body>
<div class="htad-page">
    <aside class="htad-side">
        <div class="htad-side-inner">
            <img class="htad-side-logo" src="${url.resourcesPath}/img/logo.png" alt="HTAD" />
            <h1>H&#7856;ng Tu&#7845;n An &#272;&#7841;t</h1>
            <p class="htad-tagline">Ecommerce &#8212; qu&#7843;n tr&#7883; b&#225;n h&#224;ng, kho v&#7853;n v&#224; &#273;&#417;n h&#224;ng tr&#234;n m&#7897;t n&#7873;n t&#7843;ng.</p>
            <ul class="htad-points">
                <li>Theo d&#245;i t&#7891;n kho theo th&#7901;i gian th&#7921;c</li>
                <li>Qu&#7843;n l&#253; &#273;&#417;n h&#224;ng v&#224; thanh to&#225;n t&#7853;p trung</li>
                <li>&#272;&#259;ng nh&#7853;p m&#7897;t l&#7847;n (SSO) an to&#224;n</li>
            </ul>
        </div>
    </aside>
    <main class="htad-main">
        <div class="htad-card">
            <img class="htad-logo" src="${url.resourcesPath}/img/logo.png" alt="HTAD Ecommerce" />
            <h2 class="htad-title"><#nested "header"></h2>
            <#if displayMessage && message?has_content>
                <div class="htad-alert htad-alert-${message.type}">
                    ${kcSanitize(message.summary)?no_esc}
                </div>
            </#if>
            <div class="htad-username"><#nested "show-username"></div>
            <#nested "form">
            <#if displayInfo>
                <div class="htad-info"><#nested "info"></div>
            </#if>
            <div class="htad-social"><#nested "socialProviders"></div>
            <#if realm.internationalizationEnabled && locale.supported?size gt 1>
                <div class="htad-locale">
                    <#list locale.supported as l>
                        <a href="${l.url}">${l.label}</a><#if l_has_next> &#183; </#if>
                    </#list>
                </div>
            </#if>
        </div>
        <p class="htad-foot">&copy; HTAD Ecommerce &#183; B&#7843;o m&#7853;t b&#7903;i Keycloak SSO</p>
    </main>
</div>
</body>
</html>
</#macro>
