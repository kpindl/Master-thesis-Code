SELECT id_cnv     AS id_cnv,
       id_sam     AS id_sam,
       id_csv     AS id_csv,
       identifier AS identifier,
       method     AS method,
       min_size   AS min_size,
       max_size   AS max_size,
       type       AS type,
       hgvs       AS hgvs,
       iscn       AS iscn

FROM   ngs.sam_cnv
WHERE  id_sam = ?