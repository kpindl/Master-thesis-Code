INSERT INTO ngs.sam_cnv (id_sam, id_csv, identifier, method, min_size, max_size, type, hgvs, iscn)
VALUES      (?, ?, ?, ?, ?, ?, CAST (? AS cnv_type), ?, ?);