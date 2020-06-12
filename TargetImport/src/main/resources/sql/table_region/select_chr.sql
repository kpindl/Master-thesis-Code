SELECT      region.identifier	AS identifier
        ,   symbol.name 		AS symbol
FROM        ngs.region 
INNER JOIN  ngs.symbol   	ON symbol.id_reg = region.id_reg
WHERE       region.id_typ = ?