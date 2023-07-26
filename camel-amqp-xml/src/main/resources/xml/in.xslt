<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    <xsl:template match="/">
        <list>
            <xsl:for-each select="order/item">
                <entry><xsl:value-of select="name" />: <xsl:value-of select="description" /></entry>
            </xsl:for-each>
        </list>
    </xsl:template>
</xsl:stylesheet>